package fr.miuby.survi.world;

import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.lib.utils.Rect;
import fr.miuby.lib.world.MiubyWorld;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;

public class WorldFactory {
    public WorldFactory(Server server) {
        MiubyWorld village = new MiubyWorld(server.getWorld("Village"),"Village", NamedTextColor.AQUA, EWorld.VILLAGE);
        village.setLimit(new Rect(512,-512,512,50,512,-512));
        village.setSpawnPoint(new Location(village.getWorld(), -24, 158, -30));
        WorldRegistry.register(village);

        MiubyWorld wilderness = new MiubyWorld(server.getWorld("Wilderness"),"Wilderness", NamedTextColor.GOLD, EWorld.WILDERNESS);
        wilderness.setLimit(new Rect(2000,-2000, Integer.MAX_VALUE, Integer.MIN_VALUE,2000,-2000));
        wilderness.setSpawnPoint(new Location(wilderness.getWorld(), 3, 78, -12));
        WorldRegistry.register(wilderness);

        MiubyWorld nether = new MiubyWorld(server.getWorld("Wilderness_nether"),"Nether", NamedTextColor.RED, EWorld.NETHER);
        nether.setLocked(true);
        WorldRegistry.register(nether);

        MiubyWorld end = new MiubyWorld(server.getWorld("Wilderness_the_end"),"End", NamedTextColor.YELLOW, EWorld.END);
        end.setLocked(true);
        WorldRegistry.register(end);
    }

    public static World getDefaultWorld() {
        return WorldRegistry.get(EWorld.VILLAGE).getWorld();
    }
}

