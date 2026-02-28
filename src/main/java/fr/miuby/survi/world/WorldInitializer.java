package fr.miuby.survi.world;

import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.lib.utils.Rect;
import fr.miuby.lib.world.MLWorld;
import fr.miuby.survi.GameManager;
import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;

import java.util.Map;

public final class WorldInitializer {
    @Getter
    private static final Map<EWorld, String> worlds;
    private static boolean initialized = false;

    static {
        worlds = Map.of(
                EWorld.VILLAGE, "Village",
                EWorld.WILDERNESS, "Wilderness",
                EWorld.NETHER, "Wilderness_nether",
                EWorld.END, "Wilderness_the_end"
        );
    }

    private WorldInitializer() { }

    public static void initializeWorlds() {
        Server server = GameManager.getInstance().getPlugin().getServer();

        MLWorld village = new MLWorld(server.getWorld(worlds.get(EWorld.VILLAGE)),"Village", NamedTextColor.AQUA, EWorld.VILLAGE);
        village.setLimit(new Rect(512,-512,512,50,512,-512));
        village.setSpawnPoint(new Location(village.getWorld(), -24, 158, -30));
        WorldRegistry.register(village);

        MLWorld wilderness = new MLWorld(server.getWorld(worlds.get(EWorld.WILDERNESS)),"Wilderness", NamedTextColor.GOLD, EWorld.WILDERNESS);
        wilderness.setLimit(new Rect(2000,-2000, Integer.MAX_VALUE, Integer.MIN_VALUE,2000,-2000));
        wilderness.setSpawnPoint(new Location(wilderness.getWorld(), 3, 78, -12));
        WorldRegistry.register(wilderness);

        MLWorld nether = new MLWorld(server.getWorld(worlds.get(EWorld.NETHER)),"Nether", NamedTextColor.RED, EWorld.NETHER);
        nether.setLocked(true);
        WorldRegistry.register(nether);

        MLWorld end = new MLWorld(server.getWorld(worlds.get(EWorld.END)),"End", NamedTextColor.YELLOW, EWorld.END);
        end.setLocked(true);
        WorldRegistry.register(end);
    }

    public static World getDefaultWorld() {
        return WorldRegistry.get(EWorld.VILLAGE).getWorld();
    }

    public static synchronized void initializeIfNeeded() {
        if (initialized)
            return;

        Server server = GameManager.getInstance().getPlugin().getServer();
        boolean allLoaded = worlds.values().stream().allMatch(worldName -> server.getWorld(worldName) != null);
        if (!allLoaded)
            return;

        initialized = true;
        GameManager.getInstance().initAfterWorldsLoad();
    }
}