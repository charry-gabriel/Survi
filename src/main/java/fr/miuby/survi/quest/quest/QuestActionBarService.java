package fr.miuby.survi.quest.quest;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.lang.LangService;
import net.kyori.adventure.text.Component;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Affiche la progression des quêtes journalières dans l'ActionBar.
 *
 * <p>La barre reste visible en permanence tant que la quête progresse, grâce à une
 * tâche de rafraîchissement par joueur toutes les {@value #REFRESH_TICKS} ticks.</p>
 */
public class QuestActionBarService {

    private static final long REFRESH_TICKS = 40L;
    private static final long IDLE_HIDE_MS  = 20_000L;

    private final Map<UUID, Component> activeMessages = new HashMap<>();
    private final Map<UUID, BukkitTask> refreshTasks  = new HashMap<>();
    private final Map<UUID, Long>       lastProgressAt = new HashMap<>();

    // =========================================================================
    // API publique
    // =========================================================================

    public void showProgress(AlphaPlayer player, Quest quest, PlayerQuestData data) {
        if (player.getPlayer() == null) return;
        UUID uuid = player.getUuid();

        Component message = buildProgressMessage(player, quest, data);
        activeMessages.put(uuid, message);
        lastProgressAt.put(uuid, System.currentTimeMillis());
        player.getPlayer().sendActionBar(message);

        if (!refreshTasks.containsKey(uuid)) {
            BukkitTask task = GameManager.getInstance().getScheduler().runTaskTimer(
                    GameManager.getInstance().getPlugin(),
                    () -> refreshActionBar(uuid),
                    REFRESH_TICKS, REFRESH_TICKS
            );
            refreshTasks.put(uuid, task);
        }
    }

    public void showFinished(AlphaPlayer player, Quest quest) {
        if (player.getPlayer() == null) return;
        stopRefresh(player.getUuid());
        player.getPlayer().sendActionBar(buildFinishedMessage(player, quest));
    }

    public void stopRefresh(UUID uuid) {
        activeMessages.remove(uuid);
        lastProgressAt.remove(uuid);
        BukkitTask task = refreshTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    // =========================================================================
    // Rafraîchissement
    // =========================================================================

    private void refreshActionBar(UUID uuid) {
        Component message = activeMessages.get(uuid);
        if (message == null) { stopRefresh(uuid); return; }

        Long lastTime = lastProgressAt.get(uuid);
        if (lastTime == null || System.currentTimeMillis() - lastTime > IDLE_HIDE_MS) {
            stopRefresh(uuid);
            return;
        }

        AlphaPlayer player = GameManager.getInstance().getAlphaPlayerFactory().get(uuid);
        if (player == null || player.getPlayer() == null) { stopRefresh(uuid); return; }

        player.getPlayer().sendActionBar(message);
    }

    // =========================================================================
    // Construction des messages (traductions)
    // =========================================================================

    private Component buildProgressMessage(AlphaPlayer player, Quest quest, PlayerQuestData data) {
        LangService ls = GameManager.getInstance().getLangService();
        return ls.text(player.getPlayer(), "quest.actionbar.progress",
                quest.getFormattedDescription(),
                data.getProgress() + "/" + quest.getGoal());
    }

    private Component buildFinishedMessage(AlphaPlayer player, Quest quest) {
        LangService ls = GameManager.getInstance().getLangService();
        return ls.text(player.getPlayer(), "quest.actionbar.finished", quest.getName());
    }
}