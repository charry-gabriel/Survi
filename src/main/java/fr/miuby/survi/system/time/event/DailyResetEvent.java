package fr.miuby.survi.system.time.event;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event déclenché chaque jour à 6h du matin.
 */
@Getter
public class DailyResetEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final long resetTime;       // Timestamp exact du reset
    private final int daysSinceEpoch;   // Nombre de jours depuis 01/01/1970

    public DailyResetEvent(long resetTime, int daysSinceEpoch) {
        this.resetTime = resetTime;
        this.daysSinceEpoch = daysSinceEpoch;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}