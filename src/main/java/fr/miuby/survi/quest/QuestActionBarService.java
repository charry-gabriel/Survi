package fr.miuby.survi.quest;

import fr.miuby.survi.player.AlphaPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Affiche la progression des quêtes journalières dans l'ActionBar.
 *
 * <p>La durée d'affichage est celle native de Paper (~3 secondes), sans tâche répétitive.
 * Un anti-spam de 2 secondes évite les rafraîchissements trop fréquents lorsque le joueur
 * progresse rapidement (ex. minage en masse).</p>
 *
 * <p>La complétion s'affiche toujours, sans cooldown, et réinitialise le timer pour que
 * le prochain affichage de progression soit immédiat.</p>
 */
public class QuestActionBarService {

    private static final long PROGRESS_COOLDOWN_MS = 2_000L;

    /** Timestamp du dernier affichage par joueur, pour le cooldown anti-spam. */
    private final Map<UUID, Long> lastShown = new HashMap<>();

    // =========================================================================
    // API publique
    // =========================================================================

    /**
     * Affiche la progression dans l'ActionBar, avec cooldown de 2 secondes.
     * Silencieux si le joueur n'est pas en ligne ou si le cooldown n'est pas écoulé.
     *
     * @param player joueur concerné
     * @param quest  définition de la quête
     * @param data   données de progression du joueur
     */
    public void showProgress(AlphaPlayer player, Quest quest, PlayerQuestData data) {
        if (player.getPlayer() == null) return;
        UUID uuid = player.getUuid();

        long now = System.currentTimeMillis();
        Long last = lastShown.get(uuid);
        if (last != null && (now - last) < PROGRESS_COOLDOWN_MS) return;

        lastShown.put(uuid, now);
        player.getPlayer().sendActionBar(buildProgressMessage(quest, data));
    }

    /**
     * Affiche le message de quête complétée, sans cooldown.
     * Réinitialise le cooldown pour que le prochain affichage de progression soit immédiat.
     *
     * @param player joueur concerné
     * @param quest  définition de la quête complétée
     */
    public void showCompleted(AlphaPlayer player, Quest quest) {
        if (player.getPlayer() == null) return;
        lastShown.remove(player.getUuid());
        player.getPlayer().sendActionBar(buildCompletedMessage(quest));
    }

    // =========================================================================
    // Construction des messages
    // =========================================================================

    private Component buildProgressMessage(Quest quest, PlayerQuestData data) {
        return Component.text("⚔ ", NamedTextColor.AQUA)
                .append(Component.text(quest.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" — ", NamedTextColor.DARK_GRAY))
                .append(Component.text(data.getProgress() + "/" + quest.getGoal(), NamedTextColor.WHITE));
    }

    private Component buildCompletedMessage(Quest quest) {
        return Component.text("✔ Quête terminée : ", NamedTextColor.GREEN, TextDecoration.BOLD)
                .append(Component.text(quest.getName(), NamedTextColor.GOLD))
                .append(Component.text(" — Allez voir le Trader !", NamedTextColor.GRAY));
    }
}