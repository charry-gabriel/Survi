package fr.miuby.survi.world;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public enum EWorld {
    VILLAGE,
    WILDERNESS,
    NETHER,
    END,
    ALL;

    public static EWorld get(@NotNull World world) {
        return Monde.get(world.getUID()).getType();
    }
}
