package fr.miuby.survi.player.service;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.quest.globalquest.GlobalQuest;
import fr.miuby.survi.quest.globalquest.GlobalQuestManager;
import fr.miuby.survi.blessing.BlessingEffect;
import fr.miuby.lib.villager.MLVillager;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.player.event.AlphaPlayerJobLevelUpEvent;
import fr.miuby.survi.system.database.repository.GlobalQuestPendingRewardRepository;
import fr.miuby.survi.system.database.repository.PendingJobLevelUpRepository;
import fr.miuby.survi.system.database.repository.PendingVillagerLevelUpRepository;
import fr.miuby.survi.system.lang.LangService;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import fr.miuby.survi.villager.villagerlevel.event.VillagerLevelUpEvent;
import fr.miuby.survi.world.event.WorldLevelUpEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Accumule les notifications survenues pendant qu'un joueur était déconnecté,
 * et les délivre dès sa reconnexion via {@link #deliverPending(UUID, Player)}.
 */
public class OfflineNotificationService {

    // ─── Accumulation ────────────────────────────────────────────────────────────

    public void recordJobLevelUp(AlphaPlayerJobLevelUpEvent event) {
        Player player = event.getAlphaPlayer().getPlayer();
        if (player != null && player.isOnline()) return;
        GameManager.getInstance().getDatabase().pendingJobLevelUps()
                .save(event.getAlphaPlayer().getUuid(), event.getJob(), event.getOldLevel(), event.getNewLevel());
    }

    public void recordWorldLevelUp(WorldLevelUpEvent event) {
        for (AlphaPlayer alpha : GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers()) {
            Player player = alpha.getPlayer();
            if (player != null && player.isOnline()) continue;
            GameManager.getInstance().getDatabase().pendingWorldLevelUps().save(alpha.getUuid(), event.getOldLevel());
        }
    }

    public void recordVillagerLevelUp(VillagerLevelUpEvent event) {
        String nameId   = event.getVillagerLevel().getNameId();
        int oldLevel = event.getNewLevel() - 1;
        int newLevel = event.getNewLevel();
        for (AlphaPlayer alpha : GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers()) {
            Player player = alpha.getPlayer();
            if (player != null && player.isOnline()) continue;
            GameManager.getInstance().getDatabase().pendingVillagerLevelUps().save(alpha.getUuid(), nameId, oldLevel, newLevel);
        }
    }

    /**
     * Met en attente la récompense d'une quête globale pour un joueur hors ligne, persistée en base
     * (survit à un redémarrage du serveur) et livrée à la reconnexion par {@link #deliverQuestRewards}.
     *
     * @param questId      id de la quête globale, utilisé pour retrouver et réappliquer à la reconnexion
     *                     les effets nécessitant un joueur en ligne (voir {@link BlessingEffect#requiresOnlinePlayer()})
     * @param applyRewards {@code true} si les récompenses de la quête doivent être réappliquées à la
     *                     reconnexion (complétion), {@code false} sinon (timeout : message uniquement)
     */
    public void queueQuestReward(UUID uuid, String questId, boolean applyRewards, Component rewardMessage) {
        GameManager.getInstance().getDatabase().globalQuestPendingRewards()
                .save(uuid, questId, applyRewards, GsonComponentSerializer.gson().serialize(rewardMessage));
    }

    // ─── Livraison ────────────────────────────────────────────────────────────────

    public void deliverPending(UUID uuid, Player player) {
        List<PendingJobLevelUpRepository.PendingJobLevelUp> pendingJobs =
                GameManager.getInstance().getDatabase().pendingJobLevelUps().loadForPlayer(uuid);
        Integer pendingWorldFrom =
                GameManager.getInstance().getDatabase().pendingWorldLevelUps().loadForPlayer(uuid);
        List<PendingVillagerLevelUpRepository.PendingVillagerLevelUp> pendingVillagers =
                GameManager.getInstance().getDatabase().pendingVillagerLevelUps().loadForPlayer(uuid);
        List<GlobalQuestPendingRewardRepository.PendingReward> pendingRewards =
                GameManager.getInstance().getDatabase().globalQuestPendingRewards().loadForPlayer(uuid);

        boolean hasAny = !pendingJobs.isEmpty()
                || pendingWorldFrom != null
                || !pendingVillagers.isEmpty()
                || !pendingRewards.isEmpty();

        if (hasAny) {
            player.sendMessage(GameManager.getInstance().getLangService().text(player, "offline.header"));
        }

        deliverJobNotifications(uuid, player, pendingJobs);
        deliverWorldNotification(uuid, player, pendingWorldFrom);
        deliverVillagerNotifications(uuid, player, pendingVillagers);
        deliverQuestRewards(uuid, player, pendingRewards);
        deliverGlobalQuestStatus(player);
    }

    private void deliverJobNotifications(UUID uuid, Player player, List<PendingJobLevelUpRepository.PendingJobLevelUp> pending) {
        if (pending.isEmpty()) return;

        Map<EJob, PendingJobLevelUpRepository.PendingJobLevelUp> consolidated = new LinkedHashMap<>();
        for (PendingJobLevelUpRepository.PendingJobLevelUp rec : pending) {
            consolidated.merge(rec.job(), rec, (existing, r) ->
                    new PendingJobLevelUpRepository.PendingJobLevelUp(r.job(),
                            Math.min(existing.oldLevel(), r.oldLevel()),
                            Math.max(existing.newLevel(), r.newLevel())));
        }

        LangService ls = GameManager.getInstance().getLangService();
        for (PendingJobLevelUpRepository.PendingJobLevelUp rec : consolidated.values()) {
            player.sendMessage(ls.text(player, "offline.job_level",
                    Placeholder.component("job", rec.job().toComponent()),
                    Placeholder.unparsed("old", String.valueOf(rec.oldLevel())),
                    Placeholder.unparsed("new", String.valueOf(rec.newLevel()))
            ));
        }

        GameManager.getInstance().getDatabase().pendingJobLevelUps().deleteForPlayer(uuid);
    }

    private void deliverWorldNotification(UUID uuid, Player player, Integer worldFrom) {
        if (worldFrom == null) return;
        int worldNow = GameManager.getInstance().getWorldLevelManager().getLevel();
        player.sendMessage(GameManager.getInstance().getLangService()
                .text(player, "offline.world_level", worldFrom, worldNow));
        GameManager.getInstance().getDatabase().pendingWorldLevelUps().deleteForPlayer(uuid);
    }

    private void deliverVillagerNotifications(UUID uuid, Player player, List<PendingVillagerLevelUpRepository.PendingVillagerLevelUp> pending) {
        if (pending.isEmpty()) return;

        Map<String, PendingVillagerLevelUpRepository.PendingVillagerLevelUp> consolidated = new LinkedHashMap<>();
        for (PendingVillagerLevelUpRepository.PendingVillagerLevelUp rec : pending) {
            consolidated.merge(rec.nameId(), rec, (existing, r) ->
                    new PendingVillagerLevelUpRepository.PendingVillagerLevelUp(r.nameId(),
                            Math.min(existing.oldLevel(), r.oldLevel()),
                            Math.max(existing.newLevel(), r.newLevel())));
        }

        LangService ls = GameManager.getInstance().getLangService();
        for (PendingVillagerLevelUpRepository.PendingVillagerLevelUp rec : consolidated.values()) {
            MLVillager villager = VillagerRegistry.get(rec.nameId());
            Component villagerName = villager instanceof VillagerLevel vl
                    ? vl.getDisplayName()
                    : Component.text(rec.nameId(), NamedTextColor.AQUA);

            player.sendMessage(ls.text(player, "offline.villager_level",
                    Placeholder.component("villager", villagerName),
                    Placeholder.unparsed("old", String.valueOf(rec.oldLevel())),
                    Placeholder.unparsed("new", String.valueOf(rec.newLevel()))
            ));
        }

        GameManager.getInstance().getDatabase().pendingVillagerLevelUps().deleteForPlayer(uuid);
    }

    private void deliverQuestRewards(UUID uuid, Player player, List<GlobalQuestPendingRewardRepository.PendingReward> pending) {
        if (pending.isEmpty()) return;

        AlphaPlayer ap = GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayer(uuid);
        GlobalQuestManager gqm = GameManager.getInstance().getGlobalQuestManager();

        for (GlobalQuestPendingRewardRepository.PendingReward reward : pending) {
            if (reward.applyRewards()) {
                GlobalQuest quest = gqm.getQuest(reward.questId());
                if (quest != null) {
                    for (BlessingEffect effect : quest.getRewards().blessingEffects()) {
                        if (effect.requiresOnlinePlayer()) effect.applyEffect(ap);
                    }
                } else {
                    MLLogManager.getInstance().log(Level.WARNING, ELogTag.QUEST,
                            "[OfflineNotification] Quête globale '" + reward.questId() + "' introuvable dans le pool : "
                                    + "effets différés non réappliqués pour " + ap.getPseudo() + " (" + uuid + "), message livré quand même.");
                }
            }
            player.sendMessage(GsonComponentSerializer.gson().deserialize(reward.message()));
        }

        GameManager.getInstance().getDatabase().globalQuestPendingRewards().deleteForPlayer(uuid);
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
        player.sendMessage(ls.text(player, "offline.global_quest.prefix", activeQuest.getName()));
        player.sendMessage(Component.text("   ", NamedTextColor.DARK_GRAY)
                .append(Component.text(activeQuest.getFormattedDescription(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("   ", NamedTextColor.DARK_GRAY)
                .append(ls.text(player, "offline.global_quest.progress",
                        gProgress, gGoal, percent, remaining)));
    }
}