package fr.miuby.survi.world;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.utils.Rect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class Monde {
    private final World world;
    private final String name;
    private Location spawnPoint;
    private Rect limit;

    public Monde(World world, String name) {
        this.world = world;
        this.name = name;
    }

    public World getWorld() {
        return world;
    }

    public String getName() {
        return name;
    }

    public Location getSpawnPoint() {
        return spawnPoint;
    }

    public Rect getLimit() {
        return limit;
    }

    public void setLimit(Rect limit) {
        this.limit = limit;
    }

    public void setSpawnPoint(Location spawnPoint) {
        this.spawnPoint = spawnPoint;
    }

    public static boolean isPlayerOnWorld(Player player, EWorld world) {
        return player.getWorld().getUID() == GameManager.getInstance().getWorld(world).getUID();
    }

    public static boolean isPlayerOnWorld(Player player, Monde world) {
        return player.getWorld().getUID() == world.getWorld().getUID();
    }

    public static boolean isOutOfLimit(Player player, EWorld world) {
        Block block = player.getLocation().getBlock();
        Monde monde = GameManager.getInstance().getMonde(world);
        return isPlayerOnWorld(player, monde) && monde.getLimit().isOut(block.getX(), block.getZ());
    }
}
