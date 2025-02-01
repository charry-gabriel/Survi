package fr.miuby.survi.world;

import fr.miuby.survi.utils.Rect;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorldFactory {
    private final Map<EWorld, Monde> worlds;

    public WorldFactory(Server server) {
        worlds = new HashMap<>();

        Monde village = new Monde(server.getWorld("Village"),"Village", NamedTextColor.AQUA, EWorld.VILLAGE);
        village.setLimit(new Rect(512,-512,512,50,512,-512));
        village.setSpawnPoint(new Location(village.getWorld(), -24, 158, -30));
        worlds.put(EWorld.VILLAGE, village);

        Monde wilderness = new Monde(server.getWorld("Wilderness"),"Wilderness", NamedTextColor.GOLD, EWorld.WILDERNESS);
        wilderness.setLimit(new Rect(2000,-2000, Integer.MAX_VALUE, Integer.MIN_VALUE,2000,-2000));
        wilderness.setSpawnPoint(new Location(wilderness.getWorld(), 3, 78, -12));
        worlds.put(EWorld.WILDERNESS, wilderness);

        Monde nether = new Monde(server.getWorld("Wilderness_nether"),"Nether", NamedTextColor.RED, EWorld.NETHER);
        nether.setLock(true);
        worlds.put(EWorld.NETHER, nether);

        Monde end = new Monde(server.getWorld("Wilderness_the_end"),"End", NamedTextColor.YELLOW, EWorld.END);
        end.setLock(true);
        worlds.put(EWorld.END, end);
    }

    public Monde getWorld(EWorld world) {
        Monde monde = worlds.get(world);
        if (monde == null)
            throw new NullPointerException(world.toString() + " not found !");
        return monde;
    }

    public Monde getWorld(UUID uuid) {
        for (Monde monde : worlds.values()) {
            if (monde.getUUID() == uuid) {
                return monde;
            }
        }
        throw new NullPointerException("world uuid not found !");
    }

    public static World getDefaultWorld() {
        return Monde.get(EWorld.VILLAGE).getWorld();
    }
}

