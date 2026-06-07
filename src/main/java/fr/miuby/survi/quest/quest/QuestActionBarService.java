package fr.miuby.survi.quest.quest;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Affiche la progression des quêtes journalières dans l'ActionBar.
 *
 * <p>La barre reste visible en permanence tant que la quête progresse, grâce à une
 * tâche de rafraîchissement par joueur toutes les {@value #REFRESH_TICKS} ticks
 * (inférieur à la durée native Paper de ~60 ticks, ce qui empêche la barre de disparaître
 * entre deux events de progression).</p>
 *
 * <p>Chaque progression met à jour le contenu immédiatement et réinitialise le chrono
 * d'inactivité. Si aucune progression n'est reçue pendant {@value #IDLE_HIDE_MS} ms,
 * la barre est masquée automatiquement (tâche de rafraîchissement annulée).</p>
 *
 * <p>La complétion affiche un message unique et arrête le rafraîchissement.</p>
 *
 * <p>Appeler {@link #stopRefresh(UUID)} dans tous les cas de fin de quête hors progression
 * normale : déconnexion, reset journalier, reset admin, reload.</p>
 */
public class QuestActionBarService {

    /**
     * Intervalle de rafraîchissement en ticks.
     * Doit rester inférieur à ~60 ticks (durée native Paper) pour que la barre ne clignote pas.
     */
    private static final long REFRESH_TICKS = 40L;

    /**
     * Durée d'inactivité (sans progression) au-delà de laquelle la barre est masquée automatiquement.
     * La vérification se fait à chaque tick de rafraîchissement, donc la latence réelle avant masquage
     * est au plus IDLE_HIDE_MS + (REFRESH_TICKS × 50 ms).
     */
    private static final long IDLE_HIDE_MS = 20_000L;

    /** Dernier message à afficher par joueur (mis à jour à chaque progression). */
    private final Map<UUID, Component> activeMessages = new HashMap<>();
    /** Tâche de rafraîchissement périodique par joueur. */
    private final Map<UUID, BukkitTask> refreshTasks  = new HashMap<>();
    /** Horodatage (ms) de la dernière progression reçue, par joueur. Remis à zéro par stopRefresh. */
    private final Map<UUID, Long>       lastProgressAt = new HashMap<>();

    // =========================================================================
    // API publique
    // =========================================================================

    /**
     * Affiche la progression dans l'ActionBar et maintient la barre visible en continu.
     * Chaque appel met à jour le contenu immédiatement, réinitialise le chrono d'inactivité
     * et démarre la tâche de rafraîchissement si elle n'est pas encore active.
     *
     * @param player joueur concerné
     * @param quest  définition de la quête
     * @param data   données de progression du joueur
     */
    public void showProgress(AlphaPlayer player, Quest quest, PlayerQuestData data) {
        if (player.getPlayer() == null) return;
        UUID uuid = player.getUuid();

        Component message = buildProgressMessage(quest, data);
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

    /**
     * Affiche le message de quête complétée et stoppe le rafraîchissement.
     *
     * @param player joueur concerné
     * @param quest  définition de la quête complétée
     */
    public void showCompleted(AlphaPlayer player, Quest quest) {
        if (player.getPlayer() == null) return;
        stopRefresh(player.getUuid());
        player.getPlayer().sendActionBar(buildCompletedMessage(quest));
    }

    /**
     * Arrête le rafraîchissement pour un joueur et efface le message mémorisé.
     * À appeler à la déconnexion, au reset journalier, au reset admin et au reload.
     *
     * @param uuid UUID du joueur
     */
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

        // Masquage automatique après 30 s sans progression
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
    // Construction des messages
    // =========================================================================

    private Component buildProgressMessage(Quest quest, PlayerQuestData data) {
        return Component.text("⚔ ", NamedTextColor.AQUA)
                .append(Component.text(quest.getDescription(), NamedTextColor.YELLOW))
                .append(Component.text(" — ", NamedTextColor.DARK_GRAY))
                .append(Component.text(data.getProgress() + "/" + quest.getGoal(), NamedTextColor.WHITE));
    }

    private Component buildCompletedMessage(Quest quest) {
        return Component.text("✔ Quête terminée : ", NamedTextColor.GREEN, TextDecoration.BOLD)
                .append(Component.text(quest.getName(), NamedTextColor.GOLD))
                .append(Component.text(" — Allez voir le Trader !", NamedTextColor.GRAY));
    }
}
