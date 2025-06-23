package fr.miuby.survi.world;

import fr.miuby.survi.GameManager;
import fr.miuby.utils.Rect;
import fr.miuby.world.MiubyWorld;
import fr.miuby.world.WorldRegistry;
import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import java.util.UUID;

@Getter
public class WorldFactory {
    private final WorldRegistry registry;

    public WorldFactory(Server server) {
        registry = new WorldRegistry();

        MiubyWorld village = new MiubyWorld(server.getWorld("Village"),"Village", NamedTextColor.AQUA, EWorld.VILLAGE);
        village.setLimit(new Rect(512,-512,512,50,512,-512));
        village.setSpawnPoint(new Location(village.getWorld(), -24, 158, -30));
        registry.register(village);

        MiubyWorld wilderness = new MiubyWorld(server.getWorld("Wilderness"),"Wilderness", NamedTextColor.GOLD, EWorld.WILDERNESS);
        wilderness.setLimit(new Rect(2000,-2000, Integer.MAX_VALUE, Integer.MIN_VALUE,2000,-2000));
        wilderness.setSpawnPoint(new Location(wilderness.getWorld(), 3, 78, -12));
        registry.register(wilderness);

        MiubyWorld nether = new MiubyWorld(server.getWorld("Wilderness_nether"),"Nether", NamedTextColor.RED, EWorld.NETHER);
        nether.setLocked(true);
        registry.register(nether);

        MiubyWorld end = new MiubyWorld(server.getWorld("Wilderness_the_end"),"End", NamedTextColor.YELLOW, EWorld.END);
        end.setLocked(true);
        registry.register(end);
    }

    public MiubyWorld getWorld(EWorld world) {
        MiubyWorld miubyWorld = registry.get(world);
        if (miubyWorld == null)
            throw new NullPointerException(world.toString() + " not found !");
        return miubyWorld;
    }

    public MiubyWorld getWorld(UUID uuid) {
        MiubyWorld world = registry.get(uuid);
        if (world == null)
            throw new NullPointerException("world not found !");
        return world;
    }

    public static World getDefaultWorld() {
        return get(EWorld.VILLAGE).getWorld();
    }

    public static MiubyWorld get(EWorld worldType) {
        return GameManager.getInstance().getWorldFactory().getWorld(worldType);
    }

    public static MiubyWorld get(UUID uuid) {
        return GameManager.getInstance().getWorldFactory().getWorld(uuid);
    }
}

