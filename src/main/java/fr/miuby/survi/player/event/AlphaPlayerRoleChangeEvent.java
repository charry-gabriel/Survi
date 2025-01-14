package fr.miuby.survi.player.event;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.role.Role;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @Nullable
    public Role getOldRole() {
        return oldRole;
    }

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
