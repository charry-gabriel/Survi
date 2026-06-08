package fr.miuby.survi.display;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.player.EGlobalRank;
import fr.miuby.survi.quest.quest.PlayerQuestData;
import fr.miuby.survi.quest.quest.Quest;
import fr.miuby.survi.role.Role;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.level.GameType;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import fr.miuby.survi.system.time.TimeManager;
import fr.miuby.survi.world.WorldResetManager;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Gère le tab list :
 * <ul>
 *   <li><b>Header</b> — serveur + identité du joueur</li>
 *   <li><b>Colonne gauche</b> — vrais joueurs (triés par pseudo Minecraft)</li>
 *   <li><b>Colonne droite</b> — faux joueurs : villageois + métiers (via {@link TabInfoColumn})</li>
 *   <li><b>Footer</b> — stats + quêtes actives</li>
 * </ul>
 *
 * <h3>Affichage des vrais joueurs</h3>
 * Le nom de chaque vrai joueur est remplacé via {@code UPDATE_DISPLAY_NAME} et affiche :
 * monde · sous-rôles · rôle · pseudo · PV actuels/max.
 * Aucune équipe scoreboard n'est créée pour les vrais joueurs.
 *
 * <h3>Mécanisme de tri</h3>
 * {@code listOrder} est ignoré dans Paper 26.1. Les vrais joueurs trient par pseudo Minecraft
 * (sans équipe). Les faux joueurs d'info sont dans l'équipe {@value #SORT_TEAM} dont le nom
 * commence par {@code '~'} (ASCII 126 > {@code 'z'} = 122), garantissant qu'ils
 * trient <em>après</em> tout pseudo possible → colonne droite.
 *
 * <h3>Garantie 2 colonnes</h3>
 * {@link TabInfoColumn#build(AlphaPlayer, int)} envoie {@code (40 − N)} faux joueurs
 * (padding + contenu), de sorte que N joueurs réels + (40 − N) faux = 40 entrées au total,
 * soit exactement 2 colonnes de 20 lignes.
 */
public class TabDisplayManager {

    /**
     * Nom de l'équipe scoreboard utilisée pour trier les faux joueurs après les vrais.
     * '~' (126) > 'z' (122) > toute lettre/chiffre de pseudo Minecraft → tri garanti en dernier.
     */
    private static final String SORT_TEAM = "~~tabsurvi";

    private static final EnumSet<ClientboundPlayerInfoUpdatePacket.Action> ADD_ACTIONS =
            EnumSet.of(
                    ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
                    ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED
            );

    private static final EnumSet<ClientboundPlayerInfoUpdatePacket.Action> NAME_ACTIONS =
            EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME);

    private static final Component SEP = Component.text("  ·  ", NamedTextColor.DARK_GRAY);

    /** Joueurs pour lesquels l'équipe de tri a déjà été créée cette session. */
    private final Set<UUID> teamInitialized = new HashSet<>();

    private final DecimalFormat df = new DecimalFormat("#.##");

    // ─── Init ─────────────────────────────────────────────────────────────────────

    public TabDisplayManager() {
        Bukkit.getScheduler().runTaskTimer(
                GameManager.getInstance().getPlugin(), this::updateTabList, 0L, 20L
        );
    }

    // ─── Boucle principale (1 Hz) ─────────────────────────────────────────────────

    private void updateTabList() {
        int playerCount = Bukkit.getOnlinePlayers().size();
        List<ClientboundPlayerInfoUpdatePacket.Entry> nameEntries = buildRealPlayerNameEntries();

        for (Player player : Bukkit.getOnlinePlayers()) {
            AlphaPlayer ap = AlphaPlayer.get(player.getUniqueId());
            if (ap == null || ap.getPlayer() == null) continue;

            // Création lazye de l'équipe de tri (une seule fois par session)
            if (!teamInitialized.contains(player.getUniqueId())) {
                setupSortingTeam(player);
                teamInitialized.add(player.getUniqueId());
            }

            player.sendPlayerListHeaderAndFooter(buildHeader(ap), buildFooter(ap));

            // Supprime tous les anciens faux joueurs possibles, puis renvoie les nouveaux
            sendPacket(player, new ClientboundPlayerInfoRemovePacket(TabInfoColumn.FAKE_UUIDS));
            sendPacket(player, new ClientboundPlayerInfoUpdatePacket(ADD_ACTIONS, TabInfoColumn.build(ap, playerCount)));

            // Met à jour le nom affiché de chaque vrai joueur (monde + rôle + sous-rôle + pseudo + PV)
            if (!nameEntries.isEmpty()) {
                sendPacket(player, new ClientboundPlayerInfoUpdatePacket(NAME_ACTIONS, nameEntries));
            }
        }
    }

    // ─── Noms des vrais joueurs ───────────────────────────────────────────────────

    /**
     * Construit les entrées UPDATE_DISPLAY_NAME pour tous les vrais joueurs en ligne.
     * Calculé une fois par tick et envoyé à chaque viewer.
     */
    private List<ClientboundPlayerInfoUpdatePacket.Entry> buildRealPlayerNameEntries() {
        List<ClientboundPlayerInfoUpdatePacket.Entry> entries = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            AlphaPlayer rap = GameManager.getInstance().getAlphaPlayerFactory().get(online.getUniqueId());
            if (rap == null || rap.getPlayer() == null) continue;
            entries.add(new ClientboundPlayerInfoUpdatePacket.Entry(
                    online.getUniqueId(),
                    null,
                    false,
                    0,
                    GameType.SURVIVAL,
                    PaperAdventure.asVanilla(buildPlayerTabName(rap)),
                    false,
                    0,
                    null
            ));
        }
        return entries;
    }

    /**
     * Compose le nom affiché d'un vrai joueur dans le tab :
     * [monde] [sous-rôles] [rôle] pseudo ❤ PV/PVmax
     */
    private Component buildPlayerTabName(AlphaPlayer ap) {
        Player p = ap.getPlayer();
        if (p == null) return Component.text(ap.getPseudo(), NamedTextColor.WHITE);

        NamedTextColor worldColor = (ap.getWorld() != null && ap.getWorld().getColor() != null)
                ? ap.getWorld().getColor()
                : NamedTextColor.WHITE;

        Component name = Component.empty();

        // Monde
        if (ap.getWorld() != null) {
            name = name.append(Component.text(ap.getWorld().getName() + " - ", worldColor));
        }

        // Sous-rôles
        for (Role sub : ap.getSubRoles()) {
            name = name.append(sub.displayName());
        }

        // Rôle principal
        Role role = ap.getRole();
        if (role != null) {
            name = name.append(role.displayName()).append(Component.text(" "));
        }

        // Pseudo du joueur
        name = name.append(Component.text(ap.getPseudo(), worldColor));

        return name;
    }

    // ─── Équipe de tri ────────────────────────────────────────────────────────────

    /**
     * Crée l'équipe {@value #SORT_TEAM} dans le scoreboard du joueur et y ajoute
     * les {@link TabInfoColumn#MAX_FAKE} noms de profil ({@code "s00".."s39"}).
     *
     * Le client Minecraft trie les entrées du tab par clé {@code teamName\0profileName}.
     * Comme {@value #SORT_TEAM} commence par {@code '~'}, ces entrées apparaissent
     * après tous les vrais joueurs (dont les pseudos commencent par des caractères alphanumériques).
     */
    private void setupSortingTeam(Player player) {
        Scoreboard sb = player.getScoreboard();
        if (sb.getTeam(SORT_TEAM) != null) return;

        Team team = sb.registerNewTeam(SORT_TEAM);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        for (String name : TabInfoColumn.PROFILE_NAMES) {
            team.addEntry(name);
        }
    }

    // ─── HEADER : serveur + identité ─────────────────────────────────────────────

    private Component buildHeader(AlphaPlayer ap) {
        EGlobalRank rank = ap.getGlobalRank();
        int worldLevel   = GameManager.getInstance().getWorldLevelManager().getLevel();

        // Ligne 1 : titre seul
        Component header = Component.empty()
                .appendNewline()
                .append(Component.text("◆  Survie  ◆", NamedTextColor.GOLD))
                .appendNewline();

        // Ligne 2 : monde · difficulté · reset wilderness
        header = header
                .append(Component.text(ap.getWorld().getName(), ap.getWorld().getColor()))
                .append(SEP)
                .append(Component.text("Difficulté ", NamedTextColor.GRAY))
                .append(Component.text("Niv. " + worldLevel,
                        worldLevel >= 5 ? NamedTextColor.RED : NamedTextColor.GOLD));

        Component resetComponent = buildWildernessResetLine();
        if (!resetComponent.equals(Component.empty())) {
            header = header.append(SEP).append(resetComponent);
        }
        header = header.appendNewline();

        // Ligne 3 : rang · morts · succès · rôle(s)  (sans réputation — visible dans les métiers)
        header = header
                .append(rank.displayComponent())
                .append(SEP)
                .append(Component.text("☠ " + ap.getMort(), NamedTextColor.DARK_RED))
                .append(SEP)
                .append(Component.text("★ " + ap.getSuccess(), NamedTextColor.YELLOW));

        Role mainRole = ap.getRole();
        if (mainRole != null) {
            header = header.append(SEP).append(mainRole.type().toComponent());
            List<Role> subs = ap.getSubRoles();
            for (int i = 0; i < Math.min(2, subs.size()); i++) {
                header = header.append(subs.get(i).type().toComponent());
            }
        }

        return header.appendNewline().appendNewline();
    }

    // ─── FOOTER : stats ──────────────────────────────────────────────────────────

    private Component buildFooter(AlphaPlayer alphaPlayer) {
        Player p = alphaPlayer.getPlayer();
        if (p == null) return Component.empty();

        Component footer = Component.empty()
                .appendNewline()
                .append(formatStat(alphaPlayer, Attribute.MAX_HEALTH, "Vie"))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(formatStat(alphaPlayer, Attribute.ATTACK_DAMAGE, "Force"))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(formatStat(alphaPlayer, Attribute.ARMOR, "Armure"))
                .appendNewline()
                .append(formatStat(alphaPlayer, Attribute.MOVEMENT_SPEED, "Vitesse"))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(formatStat(alphaPlayer, Attribute.ATTACK_SPEED, "Vit. d'Attaque"))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(formatStat(alphaPlayer, Attribute.LUCK, "Chance"))
                .appendNewline();

        // Description de la quête active
        PlayerQuestData questData = alphaPlayer.getCurrentActiveQuest();
        if (questData != null && !questData.isClaimed() && questData.getLastAccepted().isEqual(LocalDate.now())) {
            Quest quest = GameManager.getInstance().getQuestManager().getQuest(questData.getQuestId());
            if (quest != null) {
                footer = footer
                        .appendNewline()
                        .append(Component.text(quest.getName() + " — ", NamedTextColor.GOLD))
                        .append(Component.text(quest.getDescription(), NamedTextColor.GRAY));
            }
        }

        return footer.appendNewline();
    }

    // ─── Reset Wilderness ─────────────────────────────────────────────────────

    private Component buildWildernessResetLine() {
        WorldResetManager wrm = GameManager.getInstance().getWorldResetManager();
        TimeManager tm = GameManager.getInstance().getTimeManager();

        ZonedDateTime nextReset = wrm.getNextWildernessResetTime(tm.getServerTimezone());
        if (nextReset == null) return Component.empty(); // resets désactivés

        ZonedDateTime now = ZonedDateTime.now(tm.getServerTimezone());
        if (!now.isBefore(nextReset)) {
            return Component.text("Reset ", NamedTextColor.GRAY)
                    .append(Component.text("imminent !", NamedTextColor.YELLOW));
        }

        Duration remaining = Duration.between(now, nextReset);
        return Component.text("Reset dans ", NamedTextColor.GRAY)
                .append(Component.text(TimeManager.formatTime(remaining), NamedTextColor.YELLOW));
    }

    // ─── Nettoyage déconnexion ────────────────────────────────────────────────────

    /**
     * Appeler depuis {@code ServerListener.onPlayerQuit}.
     * Supprime les faux joueurs du tab et nettoie l'état interne.
     */
    public void removeInfoColumn(Player player) {
        sendPacket(player, new ClientboundPlayerInfoRemovePacket(TabInfoColumn.FAKE_UUIDS));
        teamInitialized.remove(player.getUniqueId());
    }

    // ─── Utilitaires ─────────────────────────────────────────────────────────────

    private void sendPacket(Player player, net.minecraft.network.protocol.Packet<?> packet) {
        ((CraftPlayer) player).getHandle().connection.send(packet);
    }

    private String formatTime(long seconds) {
        if (seconds >= 3600) return (seconds / 3600) + "h" + String.format("%02d", (seconds % 3600) / 60) + "m";
        if (seconds >= 60)   return (seconds / 60)   + "m" + String.format("%02d", seconds % 60) + "s";
        return seconds + "s";
    }

    private Component formatStat(AlphaPlayer alphaPlayer, Attribute attributeType, String statName) {
        Player player = alphaPlayer.getPlayer();
        if (player == null) return Component.empty();

        AttributeInstance attributeInstance = player.getAttribute(attributeType);
        if (attributeInstance == null) {
            return Component.empty();
        }

        double baseValue = alphaPlayer.getBaseAttributes().getOrDefault(attributeType, attributeInstance.getBaseValue());
        double currentValue = attributeInstance.getValue();
        double diff = currentValue - baseValue;

        String sign = "+";
        NamedTextColor color;
        if (Math.abs(diff) < 0.001) {
            diff = 0;
            color = NamedTextColor.GRAY;
        } else if (diff > 0) {
            color = NamedTextColor.GREEN;
            sign = "+";
        } else {
            color = NamedTextColor.RED;
            sign = "";
        }

        return Component.text(statName + ": ", NamedTextColor.WHITE)
                .append(Component.text(formatDouble(baseValue) + sign + formatDouble(diff), color));
    }

    private String formatDouble(double d) {
        return df.format(d);
    }
}