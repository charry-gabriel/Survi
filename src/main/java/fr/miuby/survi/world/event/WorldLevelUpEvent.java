package fr.miuby.survi.world.event;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Événement déclenché dans {@link fr.miuby.survi.world.WorldLevelManager#increment()}
 * lorsque le niveau global du monde augmente d'un cran.
 */
@Getter
public class WorldLevelUpEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final int oldLevel;
    private final int newLevel;

    public WorldLevelUpEvent(int oldLevel, int newLevel) {
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