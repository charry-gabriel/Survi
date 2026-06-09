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
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
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
import java.util.Map;
import java.util.Objects;
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

    /** Objectif scoreboard principal (RenderType.HEARTS, slot LIST) pour l'affichage des cœurs dans le tab. */
    private Objective healthObjective;

    /**
     * Valeurs vanilla Minecraft pour chaque attribut affiché dans le footer.
     * On ne peut pas se fier à {@code AttributeInstance.getDefaultValue()} : Paper 26.1
     * y renvoie une valeur interne qui diffère du vanilla (ex. MOVEMENT_SPEED → ~0.35
     * au lieu de 0.1), ce qui fausserait tous les pourcentages.
     */
    private static final Map<Attribute, Double> VANILLA_DEFAULTS = Map.of(
            Attribute.MAX_HEALTH,     20.0,
            Attribute.ATTACK_DAMAGE,   2.0,
            Attribute.ARMOR,           0.0,
            Attribute.MOVEMENT_SPEED,  0.1
    );

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
            AttributeInstance maxHealthAttr = p.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttr == null) continue;
            double maxHealth = maxHealthAttr.getValue();
            if (maxHealth <= 0) continue;
            int score = (int) Math.round(p.getHealth() / maxHealth * 20.0);
            healthObjective.getScore(p.getName()).setScore(Math.clamp(score, 0, 20));
        }
    }

    private void updateTabList() {
        int playerCount = Bukkit.getOnlinePlayers().size();
        updateHealthScores();
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
                .append(formatCombinedDamageStat(alphaPlayer))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(formatBlessingModifier("Résistance", alphaPlayer.getResistanceModifier()))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(formatStat(alphaPlayer, Attribute.MAX_HEALTH, "Vie"))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(formatStat(alphaPlayer, Attribute.MOVEMENT_SPEED, "Vitesse"))
                .appendNewline();

        // Quête journalière active
        PlayerQuestData questData = alphaPlayer.getCurrentActiveQuest();
        if (questData != null && !questData.isClaimed() && questData.getLastAccepted().isEqual(LocalDate.now())) {
            Quest quest = GameManager.getInstance().getQuestManager().getQuest(questData.getQuestId());
            if (quest != null) {
                int remaining = Math.max(0, quest.getGoal() - questData.getProgress());
                String description = quest.getDescription().replace("{value}", String.valueOf(remaining));
                footer = footer
                        .appendNewline()
                        .append(Component.text("Quête : ", NamedTextColor.GOLD))
                        .append(Component.text(description, NamedTextColor.GRAY));
            }
        }

        // Quête globale active
        GlobalQuestManager gqm = GameManager.getInstance().getGlobalQuestManager();
        GlobalQuest activeGlobalQuest = gqm.getActiveQuest();
        if (activeGlobalQuest != null) {
            int remaining = Math.max(0, activeGlobalQuest.getGoal() - gqm.getProgress());
            String description = activeGlobalQuest.getDescription().replace("{value}", String.valueOf(remaining));

            footer = footer
                    .appendNewline()
                    .append(Component.text("Quête globale : ", NamedTextColor.GOLD))
                    .append(Component.text(description, NamedTextColor.GRAY))
                    .append(Component.text("  ⏰ " + GlobalQuestManager.formatSeconds(gqm.getRemainingSeconds()), NamedTextColor.GRAY));
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
        if (healthObjective != null) healthObjective.getScoreboard().resetScores(player.getName());
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

    /**
     * Affiche la stat d'un attribut en pourcentage de la valeur vanilla Minecraft,
     * en ne comptant que les modificateurs apportés par notre plugin (rôles, métiers…).
     * Les modificateurs d'équipement (épée, armure portée…) sont ignorés.
     *
     * <ul>
     *   <li>Attribut à base non nulle (Vie, Force, Vitesse…) → pourcentage (100 % = vanilla).</li>
     *   <li>Attribut à base nulle avec ADD_NUMBER (Chance…) → valeur absolue (+1.0).</li>
     *   <li>Attribut à base nulle avec ADD_SCALAR uniquement (Armure…) → multiplicateur de rôle (+20%).</li>
     * </ul>
     */
    private Component formatStat(AlphaPlayer alphaPlayer, Attribute attributeType, String statName) {
        Player player = alphaPlayer.getPlayer();
        if (player == null) return Component.empty();

        AttributeInstance attr = player.getAttribute(attributeType);
        if (attr == null) return Component.empty();

        String ns = GameManager.getInstance().getPlugin().getName().toLowerCase();

        // Reconstruit la valeur effective en ne comptant que les modificateurs de notre plugin.
        // Formule Minecraft : step2 = base + Σ ADD_NUMBER ; roleValue = step2 × (1 + Σ ADD_SCALAR) × Π (1 + MULTIPLY_SCALAR_1)
        double base = attr.getBaseValue();
        double addNum = 0, addScalar = 0, multiplyTotal = 1.0;
        for (AttributeModifier mod : attr.getModifiers()) {
            if (!mod.getKey().getNamespace().equalsIgnoreCase(ns)) continue;
            switch (mod.getOperation()) {
                case ADD_NUMBER        -> addNum        += mod.getAmount();
                case ADD_SCALAR        -> addScalar     += mod.getAmount();
                case MULTIPLY_SCALAR_1 -> multiplyTotal *= 1.0 + mod.getAmount();
            }
        }
        double step2     = base + addNum;
        double roleValue = step2 * (1.0 + addScalar) * multiplyTotal;

        double vanilla = VANILLA_DEFAULTS.getOrDefault(attributeType, attr.getDefaultValue());

        // ── Attribut à base nulle (Armure, Chance…) ──────────────────────────────
        if (vanilla < 0.001) {
            if (Math.abs(roleValue) > 0.001) {
                // Contribution additive directe (ex : Chance +1.0 depuis ADD_NUMBER)
                NamedTextColor c = roleValue > 0 ? NamedTextColor.GREEN : NamedTextColor.RED;
                String sign = roleValue > 0 ? "+" : "";
                return Component.text(statName + ": ", NamedTextColor.WHITE)
                        .append(Component.text(sign + df.format(roleValue), c));
            }
            if (Math.abs(addScalar) < 0.001) {
                // Aucune modification de rôle sur cet attribut
                return Component.text(statName + ": ", NamedTextColor.WHITE)
                        .append(Component.text("—", NamedTextColor.DARK_GRAY));
            }
            // Seul un ADD_SCALAR existe (ex : Armure ×1.2 depuis les rôles)
            long scalarPct = Math.round(addScalar * 100.0);
            NamedTextColor c = addScalar > 0 ? NamedTextColor.GREEN : NamedTextColor.RED;
            String sign = addScalar > 0 ? "+" : "";
            return Component.text(statName + ": ", NamedTextColor.WHITE)
                    .append(Component.text(sign + scalarPct + "%", c));
        }

        // ── Attribut à base non nulle : pourcentage de la valeur vanilla ─────────
        double pct = roleValue / vanilla * 100.0;
        long pctRounded = Math.round(pct);
        NamedTextColor color = Math.abs(pct - 100.0) < 0.5 ? NamedTextColor.GRAY
                : pct > 100.0 ? NamedTextColor.GREEN : NamedTextColor.RED;
        return Component.text(statName + ": ", NamedTextColor.WHITE)
                .append(Component.text(pctRounded + "%", color));
    }

    /**
     * Affiche les dégâts effectifs du joueur : combinaison de l'attribut ATTACK_DAMAGE
     * (bonus de rôle/métier) et du {@code damageModifier} (bénédictions/malédictions).
     * 100 % = dégâts vanilla sans aucun modificateur de part et d'autre.
     * Vert au-dessus de 100 %, rouge en-dessous, gris à 100 %.
     */
    private Component formatCombinedDamageStat(AlphaPlayer alphaPlayer) {
        Player player = alphaPlayer.getPlayer();
        if (player == null) return Component.empty();

        AttributeInstance attr = player.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attr == null) return Component.empty();

        String ns = GameManager.getInstance().getPlugin().getName().toLowerCase();

        double base = attr.getBaseValue();
        double addNum = 0, addScalar = 0, multiplyTotal = 1.0;
        for (AttributeModifier mod : attr.getModifiers()) {
            if (!mod.getKey().getNamespace().equalsIgnoreCase(ns)) continue;
            switch (mod.getOperation()) {
                case ADD_NUMBER        -> addNum        += mod.getAmount();
                case ADD_SCALAR        -> addScalar     += mod.getAmount();
                case MULTIPLY_SCALAR_1 -> multiplyTotal *= 1.0 + mod.getAmount();
            }
        }
        double step2     = base + addNum;
        double roleValue = step2 * (1.0 + addScalar) * multiplyTotal;

        double vanilla    = VANILLA_DEFAULTS.getOrDefault(Attribute.ATTACK_DAMAGE, attr.getDefaultValue());
        double pct        = (roleValue / vanilla) * alphaPlayer.getDamageModifier() * 100.0;
        long   pctRounded = Math.round(pct);

        NamedTextColor color = Math.abs(pct - 100.0) < 0.5 ? NamedTextColor.GRAY
                : pct > 100.0 ? NamedTextColor.GREEN : NamedTextColor.RED;
        return Component.text("Dégâts: ", NamedTextColor.WHITE)
                .append(Component.text(pctRounded + "%", color));
    }

    /**
     * Affiche un modifier de blessing en pourcentage absolu (1.0 = 100 %).
     * Vert au-dessus de 100 %, rouge en-dessous, gris exactement à 100 %.
     */
    private Component formatBlessingModifier(String statName, float current) {
        long displayPct = Math.round((double) current * 100.0);
        NamedTextColor color = Math.abs(current - 1.0f) < 0.005f ? NamedTextColor.GRAY
                : current > 1.0f ? NamedTextColor.GREEN : NamedTextColor.RED;
        return Component.text(statName + ": ", NamedTextColor.WHITE)
                .append(Component.text(displayPct + "%", color));
    }
}