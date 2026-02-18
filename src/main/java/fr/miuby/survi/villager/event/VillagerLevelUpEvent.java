package fr.miuby.survi.villager.event;

import fr.miuby.survi.villager.VillagerLevel;
import lombok.Getter;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event triggered when a villager levels up.
 * This event contains information about the villager and the new level.
 */
public class VillagerLevelUpEvent extends VillagerEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();
    @Getter
    private final int newLevel;

    public VillagerLevelUpEvent(@NotNull VillagerLevel villagerLevel, int newLevel) {
        super(villagerLevel);
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
