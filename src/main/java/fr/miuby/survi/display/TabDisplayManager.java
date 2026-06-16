package fr.miuby.survi.display;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.player.EGlobalRank;
import fr.miuby.survi.quest.globalquest.GlobalQuest;
import fr.miuby.survi.quest.globalquest.GlobalQuestManager;
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
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.GameMode;

import fr.miuby.survi.system.time.TimeManager;
import fr.miuby.survi.world.WorldResetManager;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Gère le tab list :
 * <ul>
 *   <li><b>Header</b> — serveur + identité du joueur</li>
 *   <li><b>Colonne gauche</b> — vrais joueurs (triés par pseudo Minecraft)</li>
 *   <li><b>Colonne droite</b> — faux joueurs : villageois + métiers + stats (via {@link TabInfoColumn})</li>
 *   <li><b>Footer</b> — quêtes actives</li>
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

    private static final EnumSet<ClientboundPlayerInfoUpdatePacket.Action> NAME_LISTED_ACTIONS =
            EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED);

    /** Mise à jour légère : ne touche que le texte des faux joueurs déjà présents — zéro clignotement. */
    private static final EnumSet<ClientboundPlayerInfoUpdatePacket.Action> TEXT_UPDATE_ACTIONS =
            EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME);

    private static final Component SEP = Component.text("  ·  ", NamedTextColor.DARK_GRAY);

    /** Joueurs pour lesquels l'équipe de tri a déjà été créée cette session. */
    private final Set<UUID> teamInitialized = new HashSet<>();

    /** Dernier playerCount envoyé à chaque viewer — évite le remove+re-add quand le compte n'a pas changé. */
    private final Map<UUID, Integer> lastSentPlayerCount = new HashMap<>();

    /** État du chargement des skins au dernier tick — déclenche un re-add unique quand ils deviennent disponibles. */
    private boolean lastSkinLoaded = false;

    /** Objectif scoreboard principal (RenderType.HEARTS, slot LIST) pour l'affichage des cœurs dans le tab. */
    private Objective healthObjective;

    // ─── Init ─────────────────────────────────────────────────────────────────────

    public TabDisplayManager() {
        Bukkit.getScheduler().runTaskTimer(
                GameManager.getInstance().getPlugin(), this::updateTabList, 0L, 20L
        );
        initHealthObjective();
    }

    /**
     * Crée (ou recrée) l'objectif {@code survi_health} sur le scoreboard principal.
     * Aucun scoreboard par joueur n'est nécessaire : le scoreboard principal est
     * partagé par tous et l'objectif en slot {@code PLAYER_LIST} est diffusé automatiquement.
     */
    private void initHealthObjective() {
        Scoreboard sb = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
        Objective existing = sb.getObjective("survi_health");
        if (existing != null) existing.unregister();
        healthObjective = sb.registerNewObjective("survi_health", Criteria.DUMMY,
                Component.text("❤", NamedTextColor.RED), RenderType.HEARTS);
        healthObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
    }

    // ─── Boucle principale (1 Hz) ─────────────────────────────────────────────────

    /**
     * Met à jour le score (cœurs) de chaque joueur en ligne dans l'objectif {@code survi_health}.
     * Toujours 10 cœurs max : la vie actuelle est ramenée en ratio sur 20 demi-cœurs (= 10 cœurs)
     * quelle que soit la santé maximale réelle du joueur.
     * Exemple : 2 PV sur 4 max → score 10 → 5 cœurs affichés sur 10.
     */
    private void updateHealthScores() {
        if (healthObjective == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            var maxHealthAttr = p.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr == null) continue;
            double maxHealth = maxHealthAttr.getValue();
            if (maxHealth <= 0) continue;
            int score = (int) Math.round(p.getHealth() / maxHealth * 20.0);
            healthObjective.getScore(p.getName()).setScore(Math.clamp(score, 0, 20));
        }
    }

    private void updateTabList() {
        int playerCount = (int) Bukkit.getOnlinePlayers().stream().filter(p -> p.getGameMode() != GameMode.SPECTATOR).count();
        updateHealthScores();
        List<ClientboundPlayerInfoUpdatePacket.Entry> nameEntries = buildRealPlayerNameEntries();

        boolean skinsNowLoaded  = TabSkins.gray() != null;
        boolean skinStateChanged = skinsNowLoaded != lastSkinLoaded;
        lastSkinLoaded = skinsNowLoaded;

        for (Player player : Bukkit.getOnlinePlayers()) {
            AlphaPlayer ap = AlphaPlayer.get(player.getUniqueId());
            if (ap == null || ap.getPlayer() == null) continue;

            // Création lazye de l'équipe de tri (une seule fois par session)
            if (!teamInitialized.contains(player.getUniqueId())) {
                setupSortingTeam(player);
                teamInitialized.add(player.getUniqueId());
            }

            player.sendPlayerListHeaderAndFooter(buildHeader(ap), buildFooter(ap));

            List<ClientboundPlayerInfoUpdatePacket.Entry> fakeEntries = TabInfoColumn.build(ap, playerCount);
            int lastCount = lastSentPlayerCount.getOrDefault(player.getUniqueId(), -1);

            if (lastCount != playerCount || skinStateChanged) {
                // Nombre de joueurs changé ou skins fraîchement chargés : remove + re-add complet
                if (lastCount != -1) sendPacket(player, new ClientboundPlayerInfoRemovePacket(TabInfoColumn.FAKE_UUIDS));
                sendPacket(player, new ClientboundPlayerInfoUpdatePacket(ADD_ACTIONS, fakeEntries));
                lastSentPlayerCount.put(player.getUniqueId(), playerCount);
            } else {
                // Cas normal : mise à jour du texte uniquement, les entrées existent déjà côté client
                sendPacket(player, new ClientboundPlayerInfoUpdatePacket(TEXT_UPDATE_ACTIONS, fakeEntries));
            }

            // Met à jour le nom affiché de chaque vrai joueur (monde + rôle + sous-rôle + pseudo + PV)
            if (!nameEntries.isEmpty()) {
                sendPacket(player, new ClientboundPlayerInfoUpdatePacket(NAME_LISTED_ACTIONS, nameEntries));
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
                    online.getGameMode() != GameMode.SPECTATOR,
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
        var ls   = GameManager.getInstance().getLangService();
        var lang = ap.getPlayer() != null ? ls.resolveLanguage(ap.getPlayer()) : ls.getServerDefault();

        EGlobalRank rank       = ap.getGlobalRank();
        int         worldLevel = GameManager.getInstance().getWorldLevelManager().getLevel();

        // Ligne 1 : titre seul
        Component header = Component.empty()
                .appendNewline()
                .append(ls.text(lang, "tab.header.title"))
                .appendNewline();

        // Ligne 2 : monde · difficulté · reset wilderness
        NamedTextColor levelColor = worldLevel >= 5 ? NamedTextColor.RED : NamedTextColor.GOLD;
        header = header
                .append(Component.text(ap.getWorld().getName(), ap.getWorld().getColor()))
                .append(SEP)
                .append(ls.text(lang, "tab.header.difficulty_label"))
                .append(ls.text(lang, "tab.header.level", worldLevel).colorIfAbsent(levelColor));

        Component resetComponent = buildWildernessResetLine(lang);
        if (!resetComponent.equals(Component.empty())) {
            header = header.append(SEP).append(resetComponent);
        }
        header = header.appendNewline();

        // Ligne 3 : rang · morts · succès · rôle(s)
        header = header
                .append(rank.displayComponent())
                .append(SEP)
                .append(ls.text(lang, "tab.header.deaths", ap.getMort()))
                .append(SEP)
                .append(ls.text(lang, "tab.header.successes", ap.getSuccess()));

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

    // ─── FOOTER : quêtes ─────────────────────────────────────────────────────────

    private Component buildFooter(AlphaPlayer alphaPlayer) {
        var ls   = GameManager.getInstance().getLangService();
        var lang = alphaPlayer.getPlayer() != null ? ls.resolveLanguage(alphaPlayer.getPlayer()) : ls.getServerDefault();

        Component footer = Component.empty();

        // Compteur journalier (toujours visible)
        int done     = alphaPlayer.getTotalDailyQuestsClaimed();
        int capacity = GameManager.getInstance().getQuestManager().getTotalCapacity();
        footer = footer
                .append(ls.text(lang, "tab.footer.daily_label"))
                .append(Component.text(done + "/" + capacity, (capacity > 0 && done >= capacity) ? NamedTextColor.GREEN : NamedTextColor.WHITE));

        // Quête journalière active
        PlayerQuestData questData = alphaPlayer.getCurrentActiveQuest();
        if (questData != null && !questData.isClaimed() && questData.getLastAccepted().isEqual(LocalDate.now())) {
            Quest quest = GameManager.getInstance().getQuestManager().getQuest(questData.getQuestId());
            if (quest != null) {
                String description = quest.getDescription().replace("{value}", String.valueOf(quest.getGoal()));
                footer = footer
                        .appendNewline()
                        .appendNewline()
                        .append(ls.text(lang, "tab.footer.quest_label"))
                        .append(Component.text(quest.getName() + "  ", NamedTextColor.WHITE))
                        .append(Component.text(questData.getProgress() + "/" + quest.getGoal(), NamedTextColor.DARK_GRAY))
                        .appendNewline()
                        .append(Component.text(description, NamedTextColor.GRAY));
                if (questData.isCompleted()) {
                    footer = footer.append(ls.text(lang, "tab.info.completed_marker"));
                }
            }
        }

        // Quête globale active
        GlobalQuestManager gqm = GameManager.getInstance().getGlobalQuestManager();
        GlobalQuest activeGlobalQuest = gqm.getActiveQuest();
        if (activeGlobalQuest != null) {
            String description = activeGlobalQuest.getDescription().replace("{value}", String.valueOf(activeGlobalQuest.getGoal()));
            footer = footer
                    .appendNewline()
                    .appendNewline()
                    .append(ls.text(lang, "tab.footer.global_quest_label"))
                    .append(Component.text(activeGlobalQuest.getName(), NamedTextColor.WHITE))
                    .append(Component.text(gqm.getProgress() + "/" + activeGlobalQuest.getGoal(), NamedTextColor.DARK_GRAY))
                    .append(ls.text(lang, "tab.footer.global_quest_timer", GlobalQuestManager.formatSeconds(gqm.getRemainingSeconds())))
                    .appendNewline()
                    .append(Component.text(description, NamedTextColor.GRAY));
        }

        return footer.appendNewline();
    }

    // ─── Reset Wilderness ─────────────────────────────────────────────────────

    private Component buildWildernessResetLine(fr.miuby.survi.system.lang.ELang lang) {
        WorldResetManager wrm = GameManager.getInstance().getWorldResetManager();
        TimeManager tm = GameManager.getInstance().getTimeManager();

        ZonedDateTime nextReset = wrm.getNextWildernessResetTime(tm.getServerTimezone());
        if (nextReset == null) return Component.empty();

        var ls = GameManager.getInstance().getLangService();
        ZonedDateTime now = ZonedDateTime.now(tm.getServerTimezone());
        if (!now.isBefore(nextReset)) {
            return ls.text(lang, "tab.footer.reset_imminent");
        }

        Duration remaining = Duration.between(now, nextReset);
        return ls.text(lang, "tab.footer.reset_in", TimeManager.formatTime(remaining));
    }

    // ─── Nettoyage déconnexion ────────────────────────────────────────────────────

    /**
     * Appeler depuis {@code ServerListener.onPlayerQuit}.
     * Supprime les faux joueurs du tab et nettoie l'état interne.
     */
    public void removeInfoColumn(Player player) {
        sendPacket(player, new ClientboundPlayerInfoRemovePacket(TabInfoColumn.FAKE_UUIDS));
        teamInitialized.remove(player.getUniqueId());
        lastSentPlayerCount.remove(player.getUniqueId());
        if (healthObjective != null) healthObjective.getScoreboard().resetScores(player.getName());
    }

    // ─── Utilitaires ─────────────────────────────────────────────────────────────

    private void sendPacket(Player player, net.minecraft.network.protocol.Packet<?> packet) {
        ((CraftPlayer) player).getHandle().connection.send(packet);
    }
}