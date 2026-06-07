package fr.miuby.survi.quest.globalquest;

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
 * <p>La barre reste visible en permanence pendant toute la durée de la quête,
 * et se met à jour à chaque progression. Elle se masque uniquement à la fin.</p>
 *
 * <ul>
 *   <li>Démarrage → barre vide bleue, affichée immédiatement à tous</li>
 *   <li>Progression → mise à jour immédiate du contenu, barre toujours visible</li>
 *   <li>Complétion → barre verte pendant {@value #COMPLETION_DISPLAY_TICKS} ticks, puis masquée</li>
 *   <li>Annulation / timeout → masquée immédiatement</li>
 * </ul>
 *
 * <p>Les joueurs qui se connectent en cours de quête reçoivent la barre via
 * {@link #showToPlayer(Player)}, appelé depuis {@code ServerListener.onPlayerJoin}.</p>
 */
public class GlobalQuestBossBarService {

    /** Durée d'affichage de la barre verte après complétion avant masquage (10 secondes). */
    private static final long COMPLETION_DISPLAY_TICKS = 200L;

    private final BossBar bossBar = BossBar.bossBar(Component.empty(), 0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);

    private BukkitTask hideTask = null;
    /** true si une quête globale est en cours et la barre doit rester affichée en permanence. */
    private boolean active = false;

    // =========================================================================
    // API publique
    // =========================================================================

    /**
     * Appelé au démarrage d'une quête globale : affiche la barre vide à tous les joueurs.
     *
     * @param quest quête qui vient d'être lancée
     */
    public void onQuestStarted(GlobalQuest quest) {
        cancelHideTask();
        active = true;
        updateBar(buildName(quest, 0), 0f, BossBar.Color.BLUE);
        showToAll();
    }

    /**
     * Appelé à chaque unité de progression : met à jour le contenu de la barre immédiatement.
     * La barre reste visible sans interruption — aucun palier requis.
     *
     * @param quest    quête globale active (non null)
     * @param progress progression actuelle
     */
    public void onProgressUpdate(GlobalQuest quest, int progress) {
        if (!active) return;
        int goal = quest.getGoal();
        if (goal <= 0) return;
        float barProgress = Math.min(1f, (float) progress / goal);
        updateBar(buildName(quest, progress), barProgress, BossBar.Color.BLUE);
    }

    /**
     * Appelé à la complétion : affiche la barre verte, puis la masque automatiquement
     * après {@value #COMPLETION_DISPLAY_TICKS} ticks.
     *
     * @param quest quête qui vient d'être complétée
     */
    public void onQuestCompleted(GlobalQuest quest) {
        active = false;
        updateBar(buildCompletedName(quest), 1f, BossBar.Color.GREEN);
        scheduleHide(COMPLETION_DISPLAY_TICKS);
    }

    /**
     * Appelé lors d'une annulation ou d'un timeout : masque immédiatement la barre.
     */
    public void onQuestEnded() {
        active = false;
        hideNow();
    }

    /**
     * Affiche la barre au joueur qui vient de se connecter si une quête globale est active.
     * Appelé depuis {@code ServerListener.onPlayerJoin}.
     *
     * @param player joueur qui vient de se connecter
     */
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