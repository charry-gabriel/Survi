package fr.miuby.survi.player.event;

import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import lombok.Getter;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Événement déclenché lorsqu'un {@link AlphaPlayer} passe au niveau supérieur dans un métier.
 *
 * <p>Permet à tout système extérieur (effets visuels, annonces, débloquages, etc.)
 * de réagir au passage de niveau sans coupler le code dans {@code AlphaPlayer}.</p>
 *
 * <pre>{@code
 * @EventHandler
 * public void onJobLevelUp(AlphaPlayerJobLevelUpEvent event) {
 *     AlphaPlayer player = event.getAlphaPlayer();
 *     EJob        job      = event.getJob();
 *     int         newLevel = event.getNewLevel();
 * }
 * }</pre>
 */
public class AlphaPlayerJobLevelUpEvent extends AlphaPlayerEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    @Getter private final EJob job;
    @Getter private final int oldLevel;
    @Getter private final int newLevel;

    public AlphaPlayerJobLevelUpEvent(@NotNull AlphaPlayer alphaPlayer, @NotNull EJob job, int oldLevel, int newLevel) {
        super(alphaPlayer);
        this.job = job;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}