package fr.miuby.survi.quest.globalquest;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.lang.LangKey;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Affiche la progression des quêtes globales dans une barre de boss.
 */
public class GlobalQuestBossBarService {

    private static final long COMPLETION_DISPLAY_TICKS = 200L;

    private final BossBar bossBar = BossBar.bossBar(Component.empty(), 0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);

    private BukkitTask hideTask = null;
    private boolean active = false;

    // =========================================================================
    // API publique
    // =========================================================================

    public void onQuestStarted(GlobalQuest quest) {
        cancelHideTask();
        active = true;
        updateBar(buildName(quest, 0), 0f, BossBar.Color.BLUE);
        showToAll();
    }

    public void onProgressUpdate(GlobalQuest quest, int progress) {
        if (!active) return;
        int goal = quest.getGoal();
        if (goal <= 0) return;
        float barProgress = Math.min(1f, (float) progress / goal);
        updateBar(buildName(quest, progress), barProgress, BossBar.Color.BLUE);
    }

    public void onQuestFinished(GlobalQuest quest) {
        active = false;
        updateBar(buildFinishedName(quest), 1f, BossBar.Color.GREEN);
        scheduleHide(COMPLETION_DISPLAY_TICKS);
    }

    public void onQuestEnded() {
        active = false;
        hideNow();
    }

    public void showToPlayer(Player player) {
        if (active) player.showBossBar(bossBar);
    }

    // =========================================================================
    // Logique d'affichage
    // =========================================================================

    private void updateBar(Component name, float progress, BossBar.Color color) {
        bossBar.name(name);
        bossBar.progress(progress);
        bossBar.color(color);
    }

    private void showToAll() {
        for (Player p : Bukkit.getOnlinePlayers()) p.showBossBar(bossBar);
    }

    private void scheduleHide(long delayTicks) {
        cancelHideTask();
        hideTask = GameManager.getInstance().getScheduler().runTaskLater(
                GameManager.getInstance().getPlugin(),
                this::hideNow,
                delayTicks
        );
    }

    private void hideNow() {
        cancelHideTask();
        for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(bossBar);
    }

    private void cancelHideTask() {
        if (hideTask != null) { hideTask.cancel(); hideTask = null; }
    }

    // =========================================================================
    // Construction des textes (langue par défaut du serveur)
    // =========================================================================

    private Component buildName(GlobalQuest quest, int progress) {
        int goal    = quest.getGoal();
        int percent = goal > 0 ? Math.min(100, (progress * 100) / goal) : 0;
        return GameManager.getInstance().getLangService().text(
                GameManager.getInstance().getLangService().getServerDefault(),
                LangKey.QUEST_GLOBAL_BOSSBAR_CONTENT,
                quest.getName(), progress, goal, percent
        );
    }

    private Component buildFinishedName(GlobalQuest quest) {
        return GameManager.getInstance().getLangService().text(
                GameManager.getInstance().getLangService().getServerDefault(),
                LangKey.QUEST_GLOBAL_BOSSBAR_FINISHED,
                quest.getName()
        );
    }
}