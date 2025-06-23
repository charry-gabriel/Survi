package fr.miuby.survi.world;

import fr.miuby.world.WorldType;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public enum EWorld implements WorldType {
    VILLAGE,
    WILDERNESS,
    NETHER,
    END,
    ALL;

    public static EWorld get(@NotNull World world) {
        return (EWorld) WorldFactory.get(world.getUID()).getType();
    }
}
