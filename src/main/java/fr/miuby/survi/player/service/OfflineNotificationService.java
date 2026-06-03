package fr.miuby.survi.player.service;

import fr.miuby.lib.villager.MLVillager;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.JobLevelConfig;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.player.event.AlphaPlayerJobLevelUpEvent;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import fr.miuby.survi.villager.villagerlevel.event.VillagerLevelUpEvent;
import fr.miuby.survi.world.event.WorldLevelUpEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Accumule les notifications survenues pendant qu'un joueur était déconnecté,
 * et les délivre dès sa reconnexion.
 *
 * <ul>
 *   <li>Écoute {@link AlphaPlayerJobLevelUpEvent} — si le joueur est absent, met en file.</li>
 *   <li>Écoute {@link WorldLevelUpEvent} — met en file pour tous les joueurs absents.</li>
 *   <li>Écoute {@link VillagerLevelUpEvent} — met en file pour tous les joueurs absents.</li>
 *   <li>Écoute {@link PlayerJoinEvent} (HIGH) — délivre les notifications en attente.</li>
 * </ul>
 *
 * Les notifications sont purement en mémoire : elles ne survivent pas à un redémarrage.
 */
public class OfflineNotificationService implements Listener {

    private record JobLevelUpRecord(EJob job, int oldLevel, int newLevel) {}
    private record VillagerLevelUpRecord(String nameId, int oldLevel, int newLevel) {}

    /** Notifications de job en attente par joueur. */
    private final Map<UUID, List<JobLevelUpRecord>> pendingJobLevelUps = new HashMap<>();

    /**
     * Niveau du monde au premier event manqué, par joueur.
     * La valeur courante au moment du join sert de "newLevel".
     */
    private final Map<UUID, Integer> pendingWorldLevelFrom = new HashMap<>();

    /** Montées de niveau de villageois manquées, par joueur. */
    private final Map<UUID, List<VillagerLevelUpRecord>> pendingVillagerLevelUps = new HashMap<>();

    // ─── Accumulation ────────────────────────────────────────────────────────────

    @EventHandler
    public void onJobLevelUp(AlphaPlayerJobLevelUpEvent event) {
        Player player = event.getAlphaPlayer().getPlayer();
        if (player != null && player.isOnline()) return;

        UUID uuid = event.getAlphaPlayer().getUuid();
        pendingJobLevelUps.computeIfAbsent(uuid, k -> new ArrayList<>())
                .add(new JobLevelUpRecord(event.getJob(), event.getOldLevel(), event.getNewLevel()));
    }

    @EventHandler
    public void onWorldLevelUp(WorldLevelUpEvent event) {
        for (AlphaPlayer alpha : GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers()) {
            Player player = alpha.getPlayer();
            if (player != null && player.isOnline()) continue;
            // Mémorise uniquement le premier "oldLevel" manqué — le "newLevel" sera le niveau actuel au join
            pendingWorldLevelFrom.putIfAbsent(alpha.getUuid(), event.getOldLevel());
        }
    }

    @EventHandler
    public void onVillagerLevelUp(VillagerLevelUpEvent event) {
        // L'event est fired avant this.level++ dans levelUp() — oldLevel = newLevel - 1
        String nameId = event.getVillagerLevel().getNameId();
        int oldLevel = event.getNewLevel() - 1;
        int newLevel = event.getNewLevel();

        for (AlphaPlayer alpha : GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers()) {
            Player player = alpha.getPlayer();
            if (player != null && player.isOnline()) continue;
            pendingVillagerLevelUps.computeIfAbsent(alpha.getUuid(), k -> new ArrayList<>())
                    .add(new VillagerLevelUpRecord(nameId, oldLevel, newLevel));
        }
    }

    // ─── Livraison ────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Player player = event.getPlayer();

        boolean hasAny = pendingJobLevelUps.containsKey(uuid)
                || pendingWorldLevelFrom.containsKey(uuid)
                || pendingVillagerLevelUps.containsKey(uuid);

        if (hasAny) {
            player.sendMessage(Component.text("─── Pendant votre absence ───", NamedTextColor.DARK_GRAY));
        }

        deliverJobNotifications(uuid, player);
        deliverWorldNotification(uuid, player);
        deliverVillagerNotifications(uuid, player);
    }

    private void deliverJobNotifications(UUID uuid, Player player) {
        List<JobLevelUpRecord> pending = pendingJobLevelUps.remove(uuid);
        if (pending == null || pending.isEmpty()) return;

        // Consolidation : par métier, on garde le premier oldLevel et le dernier newLevel
        Map<EJob, JobLevelUpRecord> consolidated = new LinkedHashMap<>();
        for (JobLevelUpRecord rec : pending) {
            consolidated.merge(rec.job(), rec, (existing, r) ->
                    new JobLevelUpRecord(r.job(), Math.min(existing.oldLevel(), r.oldLevel()), Math.max(existing.newLevel(), r.newLevel())));
        }

        for (JobLevelUpRecord rec : consolidated.values()) {
            player.sendMessage(
                    Component.text("⚒ Métier ", NamedTextColor.GOLD)
                            .append(rec.job().toComponent())
                            .append(Component.text(" : ", NamedTextColor.GOLD))
                            .append(Component.text(JobLevelConfig.getLevelName(rec.oldLevel()), NamedTextColor.GRAY))
                            .append(Component.text(" → ", NamedTextColor.GRAY))
                            .append(Component.text(JobLevelConfig.getLevelName(rec.newLevel()), NamedTextColor.YELLOW))
            );
        }
    }

    private void deliverWorldNotification(UUID uuid, Player player) {
        Integer worldFrom = pendingWorldLevelFrom.remove(uuid);
        if (worldFrom == null) return;

        int worldNow = GameManager.getInstance().getWorldLevelManager().getLevel();
        player.sendMessage(
                Component.text("✦ Niveau du monde : ", NamedTextColor.GOLD)
                        .append(Component.text("Niveau " + worldFrom, NamedTextColor.YELLOW))
                        .append(Component.text(" → ", NamedTextColor.GRAY))
                        .append(Component.text("Niveau " + worldNow, NamedTextColor.GOLD))
        );
    }

    private void deliverVillagerNotifications(UUID uuid, Player player) {
        List<VillagerLevelUpRecord> pending = pendingVillagerLevelUps.remove(uuid);
        if (pending == null || pending.isEmpty()) return;

        // Consolidation : par villageois, on garde le premier oldLevel et le dernier newLevel
        Map<String, VillagerLevelUpRecord> consolidated = new LinkedHashMap<>();
        for (VillagerLevelUpRecord rec : pending) {
            consolidated.merge(rec.nameId(), rec, (existing, r) ->
                    new VillagerLevelUpRecord(r.nameId(), Math.min(existing.oldLevel(), r.oldLevel()), Math.max(existing.newLevel(), r.newLevel())));
        }

        for (VillagerLevelUpRecord rec : consolidated.values()) {
            MLVillager villager = VillagerRegistry.get(rec.nameId());
            Component villagerName = villager instanceof VillagerLevel vl
                    ? vl.getDisplayName()
                    : Component.text(rec.nameId(), NamedTextColor.AQUA);

            player.sendMessage(
                    Component.text("🏠 ", NamedTextColor.GOLD)
                            .append(villagerName)
                            .append(Component.text(" : ", NamedTextColor.GOLD))
                            .append(Component.text("Niveau " + rec.oldLevel(), NamedTextColor.GRAY))
                            .append(Component.text(" → ", NamedTextColor.GRAY))
                            .append(Component.text("Niveau " + rec.newLevel(), NamedTextColor.YELLOW))
            );
        }
    }
}