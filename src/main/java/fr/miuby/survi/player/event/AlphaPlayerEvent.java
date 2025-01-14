package fr.miuby.survi.player.event;

import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public abstract class AlphaPlayerEvent extends Event {
    protected AlphaPlayer alphaPlayer;

    public AlphaPlayerEvent(@NotNull AlphaPlayer alphaPlayer) {
        this.alphaPlayer = alphaPlayer;
    }

    public AlphaPlayerEvent(@NotNull AlphaPlayer alphaPlayer, boolean async) {
        super(async);
        this.alphaPlayer = alphaPlayer;
    }

    public final @NotNull AlphaPlayer getAlphaPlayer() {
        return this.alphaPlayer;
    }
}
