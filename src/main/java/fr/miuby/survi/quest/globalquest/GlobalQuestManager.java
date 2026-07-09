package fr.miuby.survi.quest.globalquest;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.blessing.BlessingEffect;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.player.AlphaPlayerFactory;
import fr.miuby.survi.player.service.OfflineNotificationService;
import fr.miuby.survi.quest.AbstractQuestManager;
import fr.miuby.survi.quest.EQuestType;
import fr.miuby.survi.quest.QuestYamlLoader;
import fr.miuby.survi.system.sound.ESound;
import fr.miuby.survi.system.sound.SoundService;
import fr.miuby.survi.system.lang.ELang;
import fr.miuby.survi.system.lang.LangService;
import fr.miuby.survi.system.log.ELogTag;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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

    /** Taille du classement mis en avant (médailles) dans les annonces de fin de quête globale. */
    private static final int TOP_SIZE = 3;
    private static final String[] TOP_MEDALS = {"🥇", "🥈", "🥉"};

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

        broadcastLocalized("globalquest.cancelled", name);
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

    /**
     * Ajuste manuellement la progression de la quête active (admin/debug).
     * Ne modifie aucune contribution individuelle — uniquement le total. Clampée à 0 minimum.
     * Termine la quête (récompenses incluses) si l'objectif est atteint.
     *
     * @return false si aucune quête n'est active
     */
    public boolean adjustProgress(int delta) {
        if (activeQuest == null) return false;

        progress = Math.max(0, progress + delta);

        if (progress >= activeQuest.getGoal()) {
            onFinished();
        } else {
            GameManager.getInstance().getGlobalQuestBossBarService().onProgressUpdate(activeQuest, progress);
        }
        return true;
    }

    /**
     * Force la progression de la quête active à une valeur précise (admin/debug).
     * Termine la quête (récompenses incluses) si la valeur atteint l'objectif.
     *
     * @return false si aucune quête n'est active
     */
    public boolean setProgress(int value) {
        if (activeQuest == null) return false;

        progress = Math.max(0, value);

        if (progress >= activeQuest.getGoal()) {
            onFinished();
        } else {
            GameManager.getInstance().getGlobalQuestBossBarService().onProgressUpdate(activeQuest, progress);
        }
        return true;
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
        List<Map.Entry<UUID, Integer>> ranked = rankContributions(snapshot);
        int worldLevel = GameManager.getInstance().getWorldLevelManager().getLevel();

        activeQuest = null;
        progress    = 0;
        endTime     = 0L;
        contributions.clear();

        Component announcement = buildQuestFinishedComponent(quest, ranked);
        broadcastQuestFinished(announcement);
        GameManager.getInstance().getGlobalQuestBossBarService().onQuestFinished(quest);

        AlphaPlayerFactory factory = GameManager.getInstance().getAlphaPlayerFactory();
        OfflineNotificationService offlineNotif = GameManager.getInstance().getOfflineNotificationService();
        LangService ls = GameManager.getInstance().getLangService();

        // Hors du top (TOP_SIZE) : le classement + nombre d'actions n'apparaît pas dans
        // l'annonce générale (qui ne montre que le top) -> message personnel dédié en plus des récompenses.
        for (int i = 0; i < ranked.size(); i++) {
            Map.Entry<UUID, Integer> entry = ranked.get(i);
            boolean isTop = i < TOP_SIZE;
            int rank      = i + 1;
            int actions   = entry.getValue();

            AlphaPlayer ap = factory.getAlphaPlayer(entry.getKey());
            Player p       = ap.getPlayer();
            boolean online = p != null && p.isOnline();

            for (BlessingEffect effect : quest.getRewards().blessingEffects()) {
                if (!effect.requiresOnlinePlayer() || online) {
                    effect.applyEffect(ap);
                }
            }

            if (online) {
                p.sendMessage(buildRewardMessage());
                if (!isTop) {
                    p.sendMessage(ls.text(p, "globalquest.rewards.rank", rank, actions));
                }
            } else {
                Component offlineMsg = announcement.append(buildRewardMessage());
                if (!isTop) {
                    offlineMsg = offlineMsg.append(ls.text(ls.getServerDefault(), "globalquest.rewards.rank", rank, actions));
                }
                offlineNotif.queueQuestReward(entry.getKey(), quest.getId(), true, offlineMsg);
            }

            MLLogManager.getInstance().log(Level.FINE, ELogTag.QUEST,
                    "[GlobalQuest] Récompense -> " + ap.getPseudo() + " (" + ap.getUuid() + ") rang=" + rank
                            + " actions=" + actions + " top=" + isTop + " online=" + online);

            // Historique persistant
            GameManager.getInstance().getDatabase().questHistory().insert(
                    ap.getUuid(), ap.getPseudo(), quest.getId(), LocalDate.now(),
                    worldLevel, null, "global", actions
            );
        }

        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                "[GlobalQuest] Quête complétée : " + quest.getId() + " | participants=" + snapshot.size());
    }

    private void onTimeout() {
        if (activeQuest == null) return;

        GlobalQuest quest = activeQuest;
        Map<UUID, Integer> snapshot = new HashMap<>(contributions);
        List<Map.Entry<UUID, Integer>> ranked = rankContributions(snapshot);

        activeQuest = null;
        progress    = 0;
        endTime     = 0L;
        contributions.clear();
        timerTask   = null;

        Component announcement = buildQuestTimeoutComponent(quest, ranked);
        broadcastMessage(announcement);
        GameManager.getInstance().getGlobalQuestBossBarService().onQuestEnded();

        notifyParticipantsOfTimeout(quest.getId(), ranked, announcement);

        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                "[GlobalQuest] Quête expirée : " + quest.getId() + " | participants=" + snapshot.size());
    }

    // =========================================================================
    // Broadcasts
    // =========================================================================

    private void broadcastQuestStart(GlobalQuest quest) {
        LangService ls      = GameManager.getInstance().getLangService();
        String      timeStr = formatSeconds(quest.getTimeLimitSeconds());

        for (Player p : Bukkit.getOnlinePlayers()) {
            ELang lang = ls.resolveLanguage(p);

            p.sendMessage(ls.text(lang, "globalquest.start",
                    Placeholder.unparsed("name",        quest.getName()),
                    Placeholder.unparsed("description", quest.getFormattedDescription()),
                    Placeholder.unparsed("goal",        String.valueOf(quest.getGoal())),
                    Placeholder.unparsed("time",        timeStr)));

            SoundService.play(p, ESound.GLOBAL_QUEST_START);
        }
    }

    private void broadcastQuestFinished(Component announcement) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(announcement);
            SoundService.play(p, ESound.GLOBAL_QUEST_COMPLETE);
        }
    }

    private Component buildQuestFinishedComponent(GlobalQuest quest, List<Map.Entry<UUID, Integer>> ranked) {
        LangService ls               = GameManager.getInstance().getLangService();
        ELang       lang             = ls.getServerDefault();
        int         participantCount = ranked.size();
        Component   top              = buildTopContributorsComponent(ranked);

        return ls.text(lang, "globalquest.complete",
                Placeholder.unparsed("name",         quest.getName()),
                Placeholder.unparsed("participants",  String.valueOf(participantCount)),
                Placeholder.component("top",          top));
    }

    private Component buildQuestTimeoutComponent(GlobalQuest quest, List<Map.Entry<UUID, Integer>> ranked) {
        LangService ls   = GameManager.getInstance().getLangService();
        ELang       lang = ls.getServerDefault();
        Component   top  = buildTopContributorsComponent(ranked);

        return ls.text(lang, "globalquest.timeout",
                Placeholder.unparsed("name", quest.getName()),
                Placeholder.component("top", top));
    }

    /** Classe les contributions par nombre d'actions décroissant. */
    private List<Map.Entry<UUID, Integer>> rankContributions(Map<UUID, Integer> snapshot) {
        return snapshot.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .toList();
    }

    /** Construit le bloc « top contributeurs » (médailles + % + actions), réutilisé par complétion et timeout. */
    private Component buildTopContributorsComponent(List<Map.Entry<UUID, Integer>> rankedEntries) {
        int totalContribution = rankedEntries.stream().mapToInt(Map.Entry::getValue).sum();
        AlphaPlayerFactory factory = GameManager.getInstance().getAlphaPlayerFactory();
        int limit = Math.min(TOP_SIZE, rankedEntries.size());

        Component top = Component.empty();
        for (int i = 0; i < limit; i++) {
            Map.Entry<UUID, Integer> entry = rankedEntries.get(i);
            String pseudo = factory.getAlphaPlayer(entry.getKey()).getPseudo();
            int pct = totalContribution > 0 ? (int) ((long) entry.getValue() * 100 / totalContribution) : 0;
            top = top.appendNewline()
                    .append(Component.text("  " + TOP_MEDALS[i] + " ", NamedTextColor.YELLOW))
                    .append(Component.text(pseudo,                   NamedTextColor.WHITE))
                    .append(Component.text("  " + pct + "%",         NamedTextColor.AQUA))
                    .append(Component.text("  (" + entry.getValue() + " actions)", NamedTextColor.DARK_GRAY));
        }
        return top;
    }

    /**
     * Notifie chaque participant du résultat de la quête après un timeout.
     * <ul>
     *   <li>En ligne, dans le top {@value #TOP_SIZE} : déjà visible dans le broadcast général ci-dessus,
     *       rien de plus à envoyer.</li>
     *   <li>En ligne, hors du top : reçoit en plus un message personnel avec son rang et son nombre
     *       d'actions.</li>
     *   <li>Hors ligne (top ou non) : reçoit à la reconnexion l'annonce complète (où son nom apparaît
     *       déjà s'il est dans le top), avec son rang/actions ajouté s'il est hors du top.
     *       Voir {@link OfflineNotificationService#deliverPending}.</li>
     * </ul>
     *
     * @param questId id de la quête qui vient d'expirer (persisté avec les annonces différées)
     */
    private void notifyParticipantsOfTimeout(String questId, List<Map.Entry<UUID, Integer>> ranked, Component announcement) {
        AlphaPlayerFactory factory = GameManager.getInstance().getAlphaPlayerFactory();
        OfflineNotificationService offlineNotif = GameManager.getInstance().getOfflineNotificationService();
        LangService ls = GameManager.getInstance().getLangService();

        for (int i = 0; i < ranked.size(); i++) {
            Map.Entry<UUID, Integer> entry = ranked.get(i);
            boolean isTop = i < TOP_SIZE;

            Player p = factory.getAlphaPlayer(entry.getKey()).getPlayer();
            boolean online = p != null && p.isOnline();

            if (isTop && online) continue; // déjà visible dans le broadcast général

            int rank  = i + 1;
            int value = entry.getValue();

            if (isTop) {
                // Hors ligne + dans le top : l'annonce (où il apparaît déjà) suffit, différée à la reconnexion.
                offlineNotif.queueQuestReward(entry.getKey(), questId, false, announcement);
            } else if (online) {
                p.sendMessage(ls.text(p, "globalquest.timeout.rank", rank, value));
            } else {
                Component rankLine = ls.text(ls.getServerDefault(), "globalquest.timeout.rank", rank, value);
                offlineNotif.queueQuestReward(entry.getKey(), questId, false, announcement.append(rankLine));
            }
        }
    }

    private Component buildRewardMessage() {
        LangService ls   = GameManager.getInstance().getLangService();
        ELang       lang = ls.getServerDefault();
        return ls.text(lang, "globalquest.rewards.message");
    }

    private void broadcastLocalized(String key, Object... args) {
        LangService ls = GameManager.getInstance().getLangService();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(ls.text(ls.resolveLanguage(p), key, args));
        }
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