package fr.miuby.survi.world;

import fr.miuby.survi.utils.Rect;
import org.bukkit.Location;
import org.bukkit.Server;
import java.util.HashMap;
import java.util.Map;

public class WorldFactory {
    private final Map<EWorld, Monde> worlds;

    public WorldFactory(Server server) {
        worlds = new HashMap<>();

        Monde village = new Monde(server.getWorld("Village"),"Village");
        village.setLimit(new Rect(11649, 12645, 1965, 1227));
        village.setSpawnPoint(new Location(server.getWorld("Village"), 12073, 64, 1463));
        worlds.put(EWorld.VILLAGE, village);

        Monde wilderness = new Monde(server.getWorld("Wilderness"),"Wilderness");
        wilderness.setLimit(new Rect(-30000000, 30000000, 30000000, -10000));
        wilderness.setSpawnPoint(new Location(server.getWorld("Wilderness"), 0, 0, 0));
        worlds.put(EWorld.WILDERNESS, wilderness);

        worlds.put(EWorld.NETHER, new Monde(server.getWorld("Wilderness_nether"),"Nether"));
        worlds.put(EWorld.END, new Monde(server.getWorld("Wilderness_the_end"),"End"));
        worlds.put(EWorld.END2, new Monde(server.getWorld("Wilderness_the_end2"),"End"));
    }

    public Monde getMonde(EWorld world) {
        Monde monde = worlds.get(world);
        if (monde == null)
            throw new NullPointerException(world.toString() + " not found !");
        return monde;
    }
}

