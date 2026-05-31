package fr.miuby.survi.display;

import com.mojang.authlib.GameProfile;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.JobLevelConfig;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.player.EGlobalRank;
import fr.miuby.survi.quest.GlobalQuest;
import fr.miuby.survi.quest.GlobalQuestManager;
import fr.miuby.survi.quest.PlayerQuestData;
import fr.miuby.survi.quest.Quest;
import fr.miuby.survi.role.Role;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class TabDisplayManager {

    // ─── UUIDs fixes — 20 spacers (col. gauche) + 20 infos (col. droite) ─────────

    private static final int COLUMN_HEIGHT = 20;

    private static final List<UUID> SPACER_UUIDS;
    private static final List<UUID> INFO_UUIDS;
    private static final List<UUID> ALL_FAKE_UUIDS;

    static {
        SPACER_UUIDS = new ArrayList<>(COLUMN_HEIGHT);
        INFO_UUIDS   = new ArrayList<>(COLUMN_HEIGHT);
        for (int i = 0; i < COLUMN_HEIGHT; i++) {
            SPACER_UUIDS.add(new UUID(0xAAAAAAAAAL, i));
            INFO_UUIDS  .add(new UUID(0xBBBBBBBBBL, i));
        }
        ALL_FAKE_UUIDS = new ArrayList<>(COLUMN_HEIGHT * 2);
        ALL_FAKE_UUIDS.addAll(SPACER_UUIDS);
        ALL_FAKE_UUIDS.addAll(INFO_UUIDS);
    }

    private static final Component SEP = Component.text("  ·  ", NamedTextColor.DARK_GRAY);

    // ─── Init ─────────────────────────────────────────────────────────────────────

    public TabDisplayManager() {
        Bukkit.getScheduler().runTaskTimer(GameManager.getInstance().getPlugin(), this::updateTabList, 0L, 20L);
    }

    // ─── Mise à jour toutes les secondes ─────────────────────────────────────────

    private void updateTabList() {
        int onlineCount = Bukkit.getOnlinePlayers().size();
        int spacerCount = Math.max(0, COLUMN_HEIGHT - onlineCount);

        for (Player player : Bukkit.getOnlinePlayers()) {
            AlphaPlayer ap = AlphaPlayer.get(player.getUniqueId());
            if (ap == null || ap.getPlayer() == null) continue;

            player.sendPlayerListHeaderAndFooter(buildHeader(ap), Component.empty());
            pushFakeEntries(player, ap, spacerCount);
        }
    }

    // ─── HEADER : serveur + identité ─────────────────────────────────────────────

    private Component buildHeader(AlphaPlayer ap) {
        EGlobalRank rank = ap.getGlobalRank();
        int worldLevel   = GameManager.getInstance().getWorldLevelManager().getLevel();

        Component header = Component.empty()
                .appendNewline()
                .append(Component.text("◆  Survi  ◆", NamedTextColor.GOLD))
                .appendNewline()
                .appendNewline();

        // Zone · Difficulté · Morts · Succès
        header = header
                .append(Component.text("Zone : ", NamedTextColor.GRAY))
                .append(Component.text(zoneName(ap), NamedTextColor.YELLOW))
                .append(SEP)
                .append(Component.text("Diff. : ", NamedTextColor.GRAY))
                .append(Component.text("Niv. " + worldLevel, worldLevel >= 5 ? NamedTextColor.RED : NamedTextColor.GOLD))
                .append(SEP)
                .append(Component.text("☠ " + ap.getMort(), NamedTextColor.DARK_RED))
                .append(SEP)
                .append(Component.text("★ " + ap.getSuccess(), NamedTextColor.YELLOW))
                .appendNewline();

        // Rang + réputation
        header = header
                .append(rank.displayComponent())
                .append(Component.text("  (" + ap.getTotalReputation() + " rép.)", rank.getColor()))
                .appendNewline();

        // Rôle principal + sous-rôles (ligne séparée)
        Role mainRole = ap.getRole();
        if (mainRole != null) {
            header = header.append(mainRole.type().toComponent());
            List<Role> subs = ap.getSubRoles();
            for (int i = 0; i < Math.min(2, subs.size()); i++) {
                header = header.append(SEP).append(subs.get(i).type().toComponent());
            }
            header = header.appendNewline();
        }

        return header;
    }

    // ─── Injection des faux joueurs ───────────────────────────────────────────────

    private void pushFakeEntries(Player player, AlphaPlayer ap, int spacerCount) {
        sendPacket(player, new ClientboundPlayerInfoRemovePacket(ALL_FAKE_UUIDS));

        List<ClientboundPlayerInfoUpdatePacket.Entry> entries = new ArrayList<>(spacerCount + COLUMN_HEIGHT);

        for (int i = 0; i < spacerCount; i++) {
            entries.add(buildSpacerEntry(i));
        }

        List<Component> lines = buildInfoLines(ap);
        for (int i = 0; i < COLUMN_HEIGHT; i++) {
            Component line = i < lines.size() ? lines.get(i) : Component.empty();
            entries.add(buildInfoEntry(i, line));
        }

        sendPacket(player, new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(
                        ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
                        ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED
                ),
                entries
        ));
    }

    private ClientboundPlayerInfoUpdatePacket.Entry buildSpacerEntry(int index) {
        UUID uuid = SPACER_UUIDS.get(index);
        // "~spXX" trie après tous les pseudos valides (~ = ASCII 126 > a-z, A-Z, 0-9, _)
        GameProfile profile = new GameProfile(uuid, "~sp" + String.format("%02d", index));
        return new ClientboundPlayerInfoUpdatePacket.Entry(
                uuid, profile, true, 0, GameType.SURVIVAL,
                net.minecraft.network.chat.Component.empty(), false, 0, null
        );
    }

    private ClientboundPlayerInfoUpdatePacket.Entry buildInfoEntry(int index, Component displayName) {
        UUID uuid = INFO_UUIDS.get(index);
        GameProfile profile = new GameProfile(uuid, "srv" + String.format("%02d", index));
        return new ClientboundPlayerInfoUpdatePacket.Entry(
                uuid, profile, true, 0, GameType.SURVIVAL,
                PaperAdventure.asVanilla(displayName), false, index + 1, null
        );
    }

    private void sendPacket(Player player, net.minecraft.network.protocol.Packet<?> packet) {
        ((CraftPlayer) player).getHandle().connection.send(packet);
    }

    // ─── Colonne droite : villageois · métiers · stats ───────────────────────────

    private List<Component> buildInfoLines(AlphaPlayer ap) {
        List<Component> lines = new ArrayList<>(COLUMN_HEIGHT);

        // ── Villageois ──
        List<Component> villagers = buildVillagerLines();
        if (!villagers.isEmpty()) {
            lines.add(Component.empty());
            lines.add(section("Villageois"));
            lines.addAll(villagers);
        }

        // ── Métiers — tous, 2 par ligne, triés niveau desc ──
        lines.add(Component.empty());
        lines.add(section("Métiers"));
        lines.addAll(buildJobsLines(ap));

        // ── Stats ──
        List<Component> stats = buildStatsLines(ap);
        if (!stats.isEmpty()) {
            lines.add(Component.empty());
            lines.add(section("Stats"));
            lines.addAll(stats);
        }

        // ── Quête du jour (uniquement si de la place reste) ──
        Component questLine  = buildQuestLine(ap);
        Component globalLine = buildGlobalQuestLine();
        if (lines.size() <= COLUMN_HEIGHT - 3 && (!questLine.equals(Component.empty()) || !globalLine.equals(Component.empty()))) {
            lines.add(Component.empty());
            lines.add(section("Quêtes"));
            if (!questLine.equals(Component.empty()))  lines.add(questLine);
            if (!globalLine.equals(Component.empty())) lines.add(globalLine);
        }

        return lines.subList(0, Math.min(lines.size(), COLUMN_HEIGHT));
    }

    // ─── Villageois ───────────────────────────────────────────────────────────────

    private List<Component> buildVillagerLines() {
        List<Component> lines = new ArrayList<>();
        for (var mlVillager : VillagerRegistry.getAll()) {
            if (mlVillager instanceof VillagerLevel vl) {
                int lvl = vl.getLevel();
                NamedTextColor lvlColor = lvl >= 3 ? NamedTextColor.GOLD : lvl >= 1 ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY;
                lines.add(Component.text(vl.getDisplayName().content() + " ", NamedTextColor.WHITE)
                        .append(Component.text("niv." + lvl, lvlColor)));
            }
        }
        return lines;
    }

    // ─── Métiers — tous les 12 en 2 par ligne ────────────────────────────────────

    private List<Component> buildJobsLines(AlphaPlayer ap) {
        EJob[] sorted = sortedJobs(ap);
        List<Component> lines = new ArrayList<>(sorted.length / 2 + 1);
        for (int i = 0; i < sorted.length; i += 2) {
            Component left = jobEntry(ap, sorted[i]);
            if (i + 1 < sorted.length) {
                lines.add(left.append(Component.text("   ", NamedTextColor.DARK_GRAY)).append(jobEntry(ap, sorted[i + 1])));
            } else {
                lines.add(left);
            }
        }
        return lines;
    }

    private Component jobEntry(AlphaPlayer ap, EJob job) {
        int level = ap.getJobLevel(job);
        NamedTextColor lvlColor = level == 0 ? NamedTextColor.DARK_GRAY
                : level >= 7 ? NamedTextColor.GOLD
                  : level >= 4 ? NamedTextColor.YELLOW
                    : NamedTextColor.WHITE;
        return job.toComponent()
                .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                .append(Component.text(JobLevelConfig.getLevelName(level), lvlColor));
    }

    private EJob[] sortedJobs(AlphaPlayer ap) {
        return Arrays.stream(EJob.values())
                .sorted((a, b) -> {
                    int diff = Integer.compare(ap.getJobLevel(b), ap.getJobLevel(a));
                    if (diff != 0) return diff;
                    return Integer.compare(ap.getJobReputation(b), ap.getJobReputation(a));
                })
                .toArray(EJob[]::new);
    }

    // ─── Stats du joueur ─────────────────────────────────────────────────────────

    /**
     * Affiche les stats Bukkit brutes.
     * TODO : remplacer par vos stats custom si AlphaPlayer en expose (défense, force, etc.)
     */
    private List<Component> buildStatsLines(AlphaPlayer ap) {
        Player p = ap.getPlayer();
        if (p == null) return List.of();

        var maxHpAttr = p.getAttribute(Attribute.MAX_HEALTH);
        var armorAttr = p.getAttribute(Attribute.ARMOR);
        var atkAttr   = p.getAttribute(Attribute.ATTACK_DAMAGE);
        var speedAttr = p.getAttribute(Attribute.MOVEMENT_SPEED);
        if (maxHpAttr == null || armorAttr == null || atkAttr == null || speedAttr == null) return List.of();

        int hp    = (int) Math.ceil(p.getHealth());
        int maxHp = (int) maxHpAttr.getValue();
        int armor = (int) armorAttr.getValue();
        int atk   = (int) atkAttr.getValue();
        int speed = (int) Math.round(speedAttr.getValue() * 1000); // 0.1 vanilla → 100

        return List.of(
                statLine("♥", hp + "/" + maxHp,    NamedTextColor.RED,   "✦", "+" + speed + " vit.", NamedTextColor.WHITE),
                statLine("❈", atk + " force",       NamedTextColor.GOLD,  "✦", armor + " déf.",       NamedTextColor.GREEN)
        );
    }

    private Component statLine(String icon1, String val1, NamedTextColor c1, String icon2, String val2, NamedTextColor c2) {
        return Component.text(icon1 + " ", c1)
                .append(Component.text(val1, c1))
                .append(Component.text("   ", NamedTextColor.DARK_GRAY))
                .append(Component.text(icon2 + " ", c2))
                .append(Component.text(val2, c2));
    }

    // ─── Quête personnelle ────────────────────────────────────────────────────────

    private Component buildQuestLine(AlphaPlayer ap) {
        PlayerQuestData data = ap.getCurrentActiveQuest();
        if (data == null || data.isClaimed()) return Component.empty();
        if (!data.getLastAccepted().isEqual(LocalDate.now())) return Component.empty();

        Quest quest = GameManager.getInstance().getQuestManager().getQuest(data.getQuestId());
        if (quest == null) return Component.empty();

        boolean done = data.isCompleted();
        int progress = data.getProgress();
        int goal     = quest.getGoal();
        NamedTextColor bar = done ? NamedTextColor.GREEN : NamedTextColor.AQUA;

        Component line = Component.text(quest.getName() + " ", NamedTextColor.WHITE)
                .append(Component.text("[", NamedTextColor.DARK_GRAY))
                .append(Component.text(progressBar(progress, goal, 8), bar))
                .append(Component.text("] " + progress + "/" + goal, NamedTextColor.DARK_GRAY));

        if (done) line = line.append(Component.text(" ✔ Trader !", NamedTextColor.GREEN));
        return line;
    }

    // ─── Quête globale ────────────────────────────────────────────────────────────

    private Component buildGlobalQuestLine() {
        GlobalQuestManager gqm = GameManager.getInstance().getGlobalQuestManager();
        GlobalQuest gq = gqm.getActiveQuest();
        if (gq == null) return Component.empty();

        int progress   = gqm.getProgress();
        int goal       = gq.getGoal();
        long remaining = gqm.getRemainingSeconds();

        return Component.text("⚔ " + gq.getName() + " ", NamedTextColor.YELLOW)
                .append(Component.text("[", NamedTextColor.DARK_GRAY))
                .append(Component.text(progressBar(progress, goal, 8), NamedTextColor.GREEN))
                .append(Component.text("]", NamedTextColor.DARK_GRAY))
                .append(Component.text(" ⏰" + formatTime(remaining), NamedTextColor.GRAY));
    }

    // ─── Nettoyage déconnexion ────────────────────────────────────────────────────

    public void removeInfoColumn(Player player) {
        sendPacket(player, new ClientboundPlayerInfoRemovePacket(ALL_FAKE_UUIDS));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private Component section(String name) {
        return Component.text("─── " + name + " ───", NamedTextColor.DARK_AQUA);
    }

    private String zoneName(AlphaPlayer ap) {
        try {
            return switch (ap.getWorld()) {
                case VILLAGE    -> "Village";
                case WILDERNESS -> "Wilderness";
                case NETHER     -> "Nether";
                case END        -> "End";
                default         -> "?";
            };
        } catch (Exception e) {
            return "?";
        }
    }

    private String progressBar(int current, int total, int width) {
        int filled = total > 0 ? Math.min(width, current * width / total) : 0;
        return "█".repeat(filled) + "░".repeat(width - filled);
    }

    private String formatTime(long seconds) {
        if (seconds >= 3600) return (seconds / 3600) + "h" + String.format("%02d", (seconds % 3600) / 60) + "m";
        if (seconds >= 60)   return (seconds / 60) + "m" + String.format("%02d", seconds % 60) + "s";
        return seconds + "s";
    }
}