package fr.miuby.survi.player.event;

import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a base class for events related to an {@link AlphaPlayer}.
 * This class serves as the foundation for any event that involves an AlphaPlayer.
 * Subclasses can provide additional context specific to their behavior.
 *
 * This class extends {@link Event} and preserves the ability to handle
 * synchronous or asynchronous event processing, as determined by the constructor.
 */
public abstract class AlphaPlayerEvent extends Event {
    protected AlphaPlayer alphaPlayer;

    protected AlphaPlayerEvent(@NotNull AlphaPlayer alphaPlayer) {
        this.alphaPlayer = alphaPlayer;
    }

    protected AlphaPlayerEvent(@NotNull AlphaPlayer alphaPlayer, boolean async) {
        super(async);
        this.alphaPlayer = alphaPlayer;
    }

    /**
     * Retrieves the AlphaPlayer instance associated with this event.
     *
     * @return the AlphaPlayer instance associated with this event, never null.
     */
    public final @NotNull AlphaPlayer getAlphaPlayer() {
        return this.alphaPlayer;
    }
}
