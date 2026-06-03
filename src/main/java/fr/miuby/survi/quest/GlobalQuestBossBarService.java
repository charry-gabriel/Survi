package fr.miuby.survi.quest;

import fr.miuby.survi.GameManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Affiche la progression des quêtes globales dans une barre de boss.
 *
 * <p>La barre s'affiche brièvement :</p>
 * <ul>
 *   <li>au lancement (0 %, barre vide)</li>
 *   <li>à chaque palier de {@value #MILESTONE_STEP} % (10 %, 20 %, …, 90 %)</li>
 *   <li>à la complétion (100 %, barre pleine verte)</li>
 * </ul>
 *
 * <p>Chaque apparition dure {@value #DISPLAY_DURATION_TICKS} ticks (15 s) puis la barre se
 * masque automatiquement. Atteindre un nouveau palier réinitialise ce délai.</p>
 */
public class GlobalQuestBossBarService {

    private static final long DISPLAY_DURATION_TICKS = 300L;  // 5 secondes
    private static final int  MILESTONE_STEP         = 10;    // palier toutes les 10 %

    private final BossBar bossBar = BossBar.bossBar(Component.empty(), 0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);

    private BukkitTask hideTask = null;
    /** Dernier palier affiché en pourcentage entier. -1 = aucune quête en cours. */
    private int lastShownMilestonePercent = -1;

    // =========================================================================
    // API publique
    // =========================================================================

    /**
     * Appelé au démarrage d'une quête globale : affiche la barre vide avec le nom.
     *
     * @param quest quête qui vient d'être lancée
     */
    public void onQuestStarted(GlobalQuest quest) {
        lastShownMilestonePercent = 0;
        updateBar(buildName(quest, 0), 0f, BossBar.Color.BLUE);
        scheduleShow();
    }

    /**
     * Appelé à chaque unité de progression : affiche la barre uniquement aux paliers de 10 %.
     *
     * @param quest    quête globale active (non null)
     * @param progress progression actuelle
     */
    public void onProgressUpdate(GlobalQuest quest, int progress) {
        int goal = quest.getGoal();
        if (goal <= 0) return;

        // Plafonné à 99 : le palier 100 % est réservé à onQuestCompleted
        int percent   = Math.min(99, (progress * 100) / goal);
        int milestone = (percent / MILESTONE_STEP) * MILESTONE_STEP;

        if (milestone <= lastShownMilestonePercent) return;
        lastShownMilestonePercent = milestone;

        float barProgress = Math.min(1f, (float) progress / goal);
        updateBar(buildName(quest, progress), barProgress, BossBar.Color.BLUE);
        scheduleShow();
    }

    /**
     * Appelé à la complétion de la quête : affiche la barre pleine en vert.
     *
     * @param quest quête qui vient d'être complétée
     */
    public void onQuestCompleted(GlobalQuest quest) {
        lastShownMilestonePercent = 100;
        updateBar(buildCompletedName(quest), 1f, BossBar.Color.GREEN);
        scheduleShow();
    }

    /**
     * Appelé lors d'une annulation ou d'un timeout : masque immédiatement la barre.
     */
    public void onQuestEnded() {
        lastShownMilestonePercent = -1;
        hideNow();
    }

    // =========================================================================
    // Logique d'affichage
    // =========================================================================

    private void updateBar(Component name, float progress, BossBar.Color color) {
        bossBar.name(name);
        bossBar.progress(progress);
        bossBar.color(color);
    }

    private void scheduleShow() {
        if (hideTask != null) { hideTask.cancel(); hideTask = null; }
        for (Player p : Bukkit.getOnlinePlayers()) p.showBossBar(bossBar);

        hideTask = GameManager.getInstance().getScheduler().runTaskLater(
                GameManager.getInstance().getPlugin(),
                this::hideNow,
                DISPLAY_DURATION_TICKS
        );
    }

    private void hideNow() {
        if (hideTask != null) { hideTask.cancel(); hideTask = null; }
        for (Player p : Bukkit.getOnlinePlayers()) p.hideBossBar(bossBar);
    }

    // =========================================================================
    // Construction des textes
    // =========================================================================

    private Component buildName(GlobalQuest quest, int progress) {
        int goal    = quest.getGoal();
        int percent = goal > 0 ? Math.min(100, (progress * 100) / goal) : 0;
        return Component.text("⚔ ", NamedTextColor.GOLD)
                .append(Component.text(quest.getName(), NamedTextColor.WHITE))
                .append(Component.text("  ▶  ", NamedTextColor.DARK_GRAY))
                .append(Component.text(progress + "/" + goal, NamedTextColor.AQUA))
                .append(Component.text("  (" + percent + "%)", NamedTextColor.GRAY));
    }

    private Component buildCompletedName(GlobalQuest quest) {
        return Component.text("✔ ", NamedTextColor.GREEN)
                .append(Component.text(quest.getName(), NamedTextColor.YELLOW))
                .append(Component.text("  —  Complétée !", NamedTextColor.GREEN));
    }
}