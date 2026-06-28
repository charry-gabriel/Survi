package fr.miuby.survi.player.event;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.player.role.Role;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an event triggered when an {@link AlphaPlayer}'s role changes.
 * This event contains information about the old role, the new role, and whether the event is cancellable.
 * It extends {@link AlphaPlayerEvent} and implements {@link Cancellable}, allowing listeners to prevent the role change.
 */
public class AlphaPlayerRoleChangeEvent extends AlphaPlayerEvent implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();
    private boolean cancelled;

    private final Role oldRole;
    private final Role newRole;

    public AlphaPlayerRoleChangeEvent(@NotNull AlphaPlayer alphaPlayer, @Nullable Role oldRole, @Nullable Role newRole) {
        super(alphaPlayer);
        this.oldRole = oldRole;
        this.newRole = newRole;
    }

    /**
     * Retrieves the previous role associated with the player before the role change event occurred.
     *
     * @return the old role of the player, or null if no previous role is specified.
     */
    @Nullable
    public Role getOldRole() {
        return oldRole;
    }

    /**
     * Retrieves the new role associated with the player after the role change event has occurred.
     *
     * @return the new role of the player, or null if no new role is specified.
     */
    @Nullable
    public Role getNewRole() {
        return newRole;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
