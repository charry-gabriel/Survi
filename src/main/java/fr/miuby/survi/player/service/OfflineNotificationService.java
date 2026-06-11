package fr.miuby.survi.player.service;

import fr.miuby.survi.quest.globalquest.GlobalQuest;
import fr.miuby.survi.quest.globalquest.GlobalQuestManager;
import fr.miuby.survi.blessing.BlessingEffect;
import fr.miuby.lib.villager.MLVillager;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.player.event.AlphaPlayerJobLevelUpEvent;
import fr.miuby.survi.system.lang.LangKey;
import fr.miuby.survi.system.lang.LangService;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import fr.miuby.survi.villager.villagerlevel.event.VillagerLevelUpEvent;
import fr.miuby.survi.world.event.WorldLevelUpEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Accumule les notifications survenues pendant qu'un joueur était déconnecté,
 * et les délivre dès sa reconnexion via {@link #deliverPending(UUID, Player)}.
 */
public class OfflineNotificationService {

    private record JobLevelUpRecord(EJob job, int oldLevel, int newLevel) {}
    private record VillagerLevelUpRecord(String nameId, int oldLevel, int newLevel) {}

    private final Map<UUID, List<JobLevelUpRecord>>      pendingJobLevelUps     = new HashMap<>();
    private final Map<UUID, Integer>                     pendingWorldLevelFrom  = new HashMap<>();
    private final Map<UUID, List<VillagerLevelUpRecord>> pendingVillagerLevelUps= new HashMap<>();

    private record PendingQuestReward(List<BlessingEffect> effects, Component message) {}
    private final Map<UUID, List<PendingQuestReward>> pendingQuestRewards = new HashMap<>();

    // ─── Accumulation ────────────────────────────────────────────────────────────

    public void recordJobLevelUp(AlphaPlayerJobLevelUpEvent event) {
        Player player = event.getAlphaPlayer().getPlayer();
        if (player != null && player.isOnline()) return;
        UUID uuid = event.getAlphaPlayer().getUuid();
        pendingJobLevelUps.computeIfAbsent(uuid, k -> new ArrayList<>())
                .add(new JobLevelUpRecord(event.getJob(), event.getOldLevel(), event.getNewLevel()));
    }

    public void recordWorldLevelUp(WorldLevelUpEvent event) {
        for (AlphaPlayer alpha : GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers()) {
            Player player = alpha.getPlayer();
            if (player != null && player.isOnline()) continue;
            pendingWorldLevelFrom.putIfAbsent(alpha.getUuid(), event.getOldLevel());
        }
    }

    public void recordVillagerLevelUp(VillagerLevelUpEvent event) {
        String nameId   = event.getVillagerLevel().getNameId();
        int oldLevel = event.getNewLevel() - 1;
        int newLevel = event.getNewLevel();
        for (AlphaPlayer alpha : GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers()) {
            Player player = alpha.getPlayer();
            if (player != null && player.isOnline()) continue;
            pendingVillagerLevelUps.computeIfAbsent(alpha.getUuid(), k -> new ArrayList<>())
                    .add(new VillagerLevelUpRecord(nameId, oldLevel, newLevel));
        }
    }

    public void queueQuestReward(UUID uuid, List<BlessingEffect> effects, Component rewardMessage) {
        pendingQuestRewards.computeIfAbsent(uuid, k -> new ArrayList<>())
                .add(new PendingQuestReward(List.copyOf(effects), rewardMessage));
    }

    // ─── Livraison ────────────────────────────────────────────────────────────────

    public void deliverPending(UUID uuid, Player player) {
        boolean hasAny = pendingJobLevelUps.containsKey(uuid)
                || pendingWorldLevelFrom.containsKey(uuid)
                || pendingVillagerLevelUps.containsKey(uuid);

        if (hasAny) {
            player.sendMessage(GameManager.getInstance().getLangService().text(player, LangKey.OFFLINE_HEADER));
        }

        deliverJobNotifications(uuid, player);
        deliverWorldNotification(uuid, player);
        deliverVillagerNotifications(uuid, player);
        deliverQuestRewards(uuid, player);
        deliverGlobalQuestStatus(player);
    }

    private void deliverJobNotifications(UUID uuid, Player player) {
        List<JobLevelUpRecord> pending = pendingJobLevelUps.remove(uuid);
        if (pending == null || pending.isEmpty()) return;

        Map<EJob, JobLevelUpRecord> consolidated = new LinkedHashMap<>();
        for (JobLevelUpRecord rec : pending) {
            consolidated.merge(rec.job(), rec, (existing, r) ->
                    new JobLevelUpRecord(r.job(),
                            Math.min(existing.oldLevel(), r.oldLevel()),
                            Math.max(existing.newLevel(), r.newLevel())));
        }

        LangService ls = GameManager.getInstance().getLangService();
        for (JobLevelUpRecord rec : consolidated.values()) {
            player.sendMessage(ls.text(player, LangKey.OFFLINE_JOB_LEVEL,
                    Placeholder.component("job", rec.job().toComponent()),
                    Placeholder.unparsed("old", String.valueOf(rec.oldLevel())),
                    Placeholder.unparsed("new", String.valueOf(rec.newLevel()))
            ));
        }
    }

    private void deliverWorldNotification(UUID uuid, Player player) {
        Integer worldFrom = pendingWorldLevelFrom.remove(uuid);
        if (worldFrom == null) return;
        int worldNow = GameManager.getInstance().getWorldLevelManager().getLevel();
        player.sendMessage(GameManager.getInstance().getLangService()
                .text(player, LangKey.OFFLINE_WORLD_LEVEL, worldFrom, worldNow));
    }

    private void deliverVillagerNotifications(UUID uuid, Player player) {
        List<VillagerLevelUpRecord> pending = pendingVillagerLevelUps.remove(uuid);
        if (pending == null || pending.isEmpty()) return;

        Map<String, VillagerLevelUpRecord> consolidated = new LinkedHashMap<>();
        for (VillagerLevelUpRecord rec : pending) {
            consolidated.merge(rec.nameId(), rec, (existing, r) ->
                    new VillagerLevelUpRecord(r.nameId(),
                            Math.min(existing.oldLevel(), r.oldLevel()),
                            Math.max(existing.newLevel(), r.newLevel())));
        }

        LangService ls = GameManager.getInstance().getLangService();
        for (VillagerLevelUpRecord rec : consolidated.values()) {
            MLVillager villager = VillagerRegistry.get(rec.nameId());
            Component villagerName = villager instanceof VillagerLevel vl
                    ? vl.getDisplayName()
                    : Component.text(rec.nameId(), NamedTextColor.AQUA);

            player.sendMessage(ls.text(player, LangKey.OFFLINE_VILLAGER_LEVEL,
                    Placeholder.component("villager", villagerName),
                    Placeholder.unparsed("old", String.valueOf(rec.oldLevel())),
                    Placeholder.unparsed("new", String.valueOf(rec.newLevel()))
            ));
        }
    }

    private void deliverQuestRewards(UUID uuid, Player player) {
        List<PendingQuestReward> pending = pendingQuestRewards.remove(uuid);
        if (pending == null || pending.isEmpty()) return;
        AlphaPlayer ap = GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayer(uuid);
        for (PendingQuestReward reward : pending) {
            for (BlessingEffect effect : reward.effects()) effect.applyEffect(ap);
            player.sendMessage(reward.message());
        }
    }

    private void deliverGlobalQuestStatus(Player player) {
        GlobalQuestManager gqm = GameManager.getInstance().getGlobalQuestManager();
        GlobalQuest activeQuest = gqm.getActiveQuest();
        if (activeQuest == null) return;

        int gProgress = gqm.getProgress();
        int gGoal     = activeQuest.getGoal();
        int percent   = gGoal > 0 ? Math.min(100, (gProgress * 100) / gGoal) : 0;
        String remaining = GlobalQuestManager.formatSeconds(gqm.getRemainingSeconds());

        LangService ls = GameManager.getInstance().getLangService();
        player.sendMessage(ls.text(player, LangKey.OFFLINE_GLOBAL_QUEST_PREFIX, activeQuest.getName()));
        player.sendMessage(Component.text("   ", NamedTextColor.DARK_GRAY)
                .append(Component.text(activeQuest.getFormattedDescription(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("   ", NamedTextColor.DARK_GRAY)
                .append(ls.text(player, LangKey.OFFLINE_GLOBAL_QUEST_PROGRESS,
                        gProgress, gGoal, percent, remaining)));
    }
}