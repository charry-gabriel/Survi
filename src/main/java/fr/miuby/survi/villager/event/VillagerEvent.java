package fr.miuby.survi.villager.event;

import fr.miuby.survi.villager.VillagerLevel;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for events related to {@link VillagerLevel}.
 */
public abstract class VillagerEvent extends Event {
    protected final VillagerLevel villagerLevel;

    protected VillagerEvent(@NotNull VillagerLevel villagerLevel) {
        this.villagerLevel = villagerLevel;
    }

    @NotNull
    public VillagerLevel getVillagerLevel() {
        return villagerLevel;
    }
}
