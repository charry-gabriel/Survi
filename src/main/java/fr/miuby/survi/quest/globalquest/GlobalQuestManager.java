package fr.miuby.survi.quest.globalquest;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.blessing.BlessingEffect;
import fr.miuby.survi.blessing.ReputationEffect;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.player.AlphaPlayerFactory;
import fr.miuby.survi.player.service.OfflineNotificationService;
import fr.miuby.survi.quest.AbstractQuestManager;
import fr.miuby.survi.quest.EQuestType;
import fr.miuby.survi.quest.QuestYamlLoader;
import fr.miuby.survi.sound.ESound;
import fr.miuby.survi.sound.SoundService;
import fr.miuby.survi.system.log.ELogTag;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;

public class GlobalQuestManager extends AbstractQuestManager<GlobalQuest> {

    @Getter private GlobalQuest activeQuest = null;
    @Getter private int progress = 0;
    /** Contribution individuelle de chaque participant : UUID → nombre d'actions effectuées. */
    private final Map<UUID, Integer> contributions = new HashMap<>();

    private BukkitTask timerTask = null;
    private long endTime = 0L;

    public GlobalQuestManager() {
        loadQuests();
    }

    // =========================================================================
    // Pool — source YAML
    // =========================================================================

    @Override
    protected List<GlobalQuest> fetchPool() {
        return QuestYamlLoader.loadGlobalQuests();
    }

    // =========================================================================
    // Reload à chaud
    // =========================================================================

    /**
     * Recharge le pool de quêtes globales depuis {@code global_quests.yml} à chaud.
     *
     * <h3>Comportement sur la quête active</h3>
     * <ul>
     *   <li>La quête active en cours <b>n'est pas interrompue</b> — elle continue jusqu'à
     *       complétion ou timeout.</li>
     *   <li>Si son {@code id} n'existe plus dans le nouveau fichier, un avertissement est
     *       loggé mais aucune action n'est prise (la définition en mémoire reste valide
     *       jusqu'à la fin de la quête).</li>
     *   <li>Si son {@code id} existe encore, la définition est silencieusement mise à jour
     *       pour les futures références. La progression en cours reste inchangée.</li>
     * </ul>
     *
     * @return le nombre de quêtes présentes dans le pool après rechargement
     */
    public int reload() {
        loadQuests();

        if (activeQuest != null && getQuest(activeQuest.getId()) == null) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.QUEST,
                    "[GlobalQuest Reload] La quête active '" + activeQuest.getId()
                            + "' est introuvable dans le nouveau global_quests.yml. "
                            + "Elle continuera jusqu'à sa fin naturelle (complétion ou timeout).");
        }

        return questPool.size();
    }

    // =========================================================================
    // API publique
    // =========================================================================

    /** Ensemble des UUID ayant contribué à la quête en cours. */
    public Set<UUID> getParticipants() { return contributions.keySet(); }

    /** Snapshot en lecture seule des contributions individuelles (UUID → actions). */
    public Map<UUID, Integer> getContributions() { return Collections.unmodifiableMap(contributions); }

    public boolean startQuest(String questId) {
        if (activeQuest != null) return false;

        GlobalQuest quest = getQuest(questId);
        if (quest == null) return false;

        activeQuest = quest;
        progress    = 0;
        contributions.clear();
        endTime = System.currentTimeMillis() + quest.getTimeLimitSeconds() * 1000L;

        broadcastQuestStart(quest);

        timerTask = GameManager.getInstance().getScheduler().runTaskLater(
                GameManager.getInstance().getPlugin(),
                this::onTimeout,
                (long) quest.getTimeLimitSeconds() * 20L
        );

        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                "[GlobalQuest] Quête démarrée : " + quest.getId());
        GameManager.getInstance().getGlobalQuestBossBarService().onQuestStarted(quest);
        return true;
    }

    public void cancelQuest() {
        if (activeQuest == null) return;
        if (timerTask != null) { timerTask.cancel(); timerTask = null; }

        String name = activeQuest.getName();
        activeQuest = null;
        progress    = 0;
        endTime     = 0L;
        contributions.clear();

        broadcastMessage(Component.text("⚔ Quête Globale ", NamedTextColor.RED, TextDecoration.BOLD)
                .append(Component.text("« " + name + " » annulée par un administrateur. Aucune récompense.", NamedTextColor.YELLOW)));
        GameManager.getInstance().getGlobalQuestBossBarService().onQuestEnded();

        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                "[GlobalQuest] Quête annulée par admin : " + name);
    }

    public void progressGlobalQuest(AlphaPlayer player, EQuestType type, Object target, int amount) {
        if (activeQuest == null) return;
        if (!activeQuest.matchesAction(type, target)) return;

        contributions.merge(player.getUuid(), amount, Integer::sum);
        progress = Math.min(progress + amount, activeQuest.getGoal() + amount);

        if (progress >= activeQuest.getGoal()) {
            onFinished();
        } else {
            GameManager.getInstance().getGlobalQuestBossBarService().onProgressUpdate(activeQuest, progress);
        }
    }

    public long getRemainingSeconds() {
        if (activeQuest == null) return 0L;
        return Math.max(0L, (endTime - System.currentTimeMillis()) / 1000L);
    }

    // =========================================================================
    // Logique interne
    // =========================================================================

    private void onFinished() {
        if (timerTask != null) { timerTask.cancel(); timerTask = null; }

        GlobalQuest quest = activeQuest;
        Map<UUID, Integer> snapshot = new HashMap<>(contributions);
        int worldLevel = GameManager.getInstance().getWorldLevelManager().getLevel();

        activeQuest = null;
        progress    = 0;
        endTime     = 0L;
        contributions.clear();

        broadcastQuestFinished(quest, snapshot);
        GameManager.getInstance().getGlobalQuestBossBarService().onQuestFinished(quest);

        AlphaPlayerFactory factory = GameManager.getInstance().getAlphaPlayerFactory();
        OfflineNotificationService offlineNotif = GameManager.getInstance().getOfflineNotificationService();

        for (Map.Entry<UUID, Integer> entry : snapshot.entrySet()) {
            AlphaPlayer ap = factory.getAlphaPlayer(entry.getKey());
            Player p       = ap.getPlayer();
            boolean online = p != null && p.isOnline();

            List<BlessingEffect> deferred = new ArrayList<>();
            for (BlessingEffect effect : quest.getRewards().blessingEffects()) {
                if (!effect.requiresOnlinePlayer() || online) {
                    effect.applyEffect(ap);
                } else {
                    deferred.add(effect);
                }
            }

            if (!deferred.isEmpty()) offlineNotif.queueQuestReward(entry.getKey(), deferred, buildRewardMessage(quest));
            if (online) p.sendMessage(buildRewardMessage(quest));

            // Historique persistant
            GameManager.getInstance().getDatabase().questHistory().insert(
                    ap.getUuid(), ap.getPseudo(), quest.getId(), LocalDate.now(),
                    worldLevel, null, "global", entry.getValue()
            );
        }

        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                "[GlobalQuest] Quête complétée : " + quest.getId() + " | participants=" + snapshot.size());
    }

    private void onTimeout() {
        if (activeQuest == null) return;
        String name = activeQuest.getName();

        activeQuest = null;
        progress    = 0;
        endTime     = 0L;
        contributions.clear();
        timerTask   = null;

        broadcastMessage(
                Component.text("⏰ Quête Globale ", NamedTextColor.RED, TextDecoration.BOLD)
                        .append(Component.text("« " + name + " »", NamedTextColor.YELLOW))
                        .append(Component.text(" non complétée dans les temps ! Aucune récompense.", NamedTextColor.RED))
        );
        GameManager.getInstance().getGlobalQuestBossBarService().onQuestEnded();

        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                "[GlobalQuest] Quête expirée : " + name);
    }

    // =========================================================================
    // Broadcasts
    // =========================================================================

    private void broadcastQuestStart(GlobalQuest quest) {
        String timeStr = formatSeconds(quest.getTimeLimitSeconds());

        Component msg = Component.newline()
                .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
                .appendNewline()
                .append(Component.text("  ⚔  QUÊTE GLOBALE LANCÉE  ⚔", NamedTextColor.GOLD, TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text("  " + quest.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text("  " + quest.getFormattedDescription(), NamedTextColor.WHITE))
                .appendNewline()
                .append(Component.text("  Objectif : ", NamedTextColor.GRAY))
                .append(Component.text(quest.getGoal() + " ", NamedTextColor.AQUA))
                .append(Component.text("| Temps : ", NamedTextColor.GRAY))
                .append(Component.text(timeStr, NamedTextColor.AQUA))
                .appendNewline()
                .append(Component.text("  Récompenses :", NamedTextColor.GRAY));

        for (BlessingEffect effect : quest.getRewards().blessingEffects()) {
            if (effect instanceof ReputationEffect re) {
                msg = msg.appendNewline()
                        .append(Component.text("    +" + re.getReputation() + " rép. ", NamedTextColor.GREEN))
                        .append(re.getJob().toComponent());
            }
        }

        msg = msg.appendNewline()
                .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD))
                .appendNewline();

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(msg);
            SoundService.play(p, ESound.GLOBAL_QUEST_START);
        }
    }

    private void broadcastQuestFinished(GlobalQuest quest, Map<UUID, Integer> snapshot) {
        int participantCount  = snapshot.size();
        int totalContribution = snapshot.values().stream().mapToInt(Integer::intValue).sum();

        // Top 3 contributeurs triés par contribution décroissante
        List<Map.Entry<UUID, Integer>> top3 = snapshot.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(3)
                .toList();

        AlphaPlayerFactory factory = GameManager.getInstance().getAlphaPlayerFactory();
        String[] medals = {"🥇", "🥈", "🥉"};

        Component msg = Component.newline()
                .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GREEN))
                .appendNewline()
                .append(Component.text("  ✔  QUÊTE GLOBALE COMPLÉTÉE  ✔", NamedTextColor.GREEN, TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text("  " + quest.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD))
                .appendNewline()
                .append(Component.text("  " + participantCount + " participant(s) récompensé(s) !", NamedTextColor.WHITE));

        if (!top3.isEmpty()) {
            msg = msg.appendNewline()
                    .append(Component.text("  ─ Top contributeurs ─", NamedTextColor.DARK_GRAY));
            for (int i = 0; i < top3.size(); i++) {
                Map.Entry<UUID, Integer> entry = top3.get(i);
                String pseudo = factory.getAlphaPlayer(entry.getKey()).getPseudo();
                int pct = totalContribution > 0 ? (int) ((long) entry.getValue() * 100 / totalContribution) : 0;
                msg = msg.appendNewline()
                        .append(Component.text("  " + medals[i] + " ", NamedTextColor.YELLOW))
                        .append(Component.text(pseudo, NamedTextColor.WHITE))
                        .append(Component.text("  " + pct + "%", NamedTextColor.AQUA))
                        .append(Component.text("  (" + entry.getValue() + " actions)", NamedTextColor.DARK_GRAY));
            }
        }

        msg = msg.appendNewline()
                .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GREEN))
                .appendNewline();

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(msg);
            SoundService.play(p, ESound.GLOBAL_QUEST_COMPLETE);
        }
    }

    private Component buildRewardMessage(GlobalQuest quest) {
        Component msg = Component.text("[Quête Globale] ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text("Récompenses reçues :", NamedTextColor.GREEN));
        for (BlessingEffect effect : quest.getRewards().blessingEffects()) {
            if (effect instanceof ReputationEffect re) {
                msg = msg.append(Component.text(" +" + re.getReputation(), NamedTextColor.GREEN))
                        .append(Component.text(" rép. ", NamedTextColor.GRAY))
                        .append(re.getJob().toComponent());
            }
        }
        return msg;
    }

    private void broadcastMessage(Component msg) {
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(msg);
    }

    // =========================================================================
    // Utilitaires
    // =========================================================================

    public static String formatSeconds(long totalSeconds) {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        if (h > 0) return String.format("%dh%02dm%02ds", h, m, s);
        if (m > 0) return String.format("%dm%02ds", m, s);
        return s + "s";
    }
}