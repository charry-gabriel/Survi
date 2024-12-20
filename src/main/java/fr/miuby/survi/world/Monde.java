package fr.miuby.survi.world;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.utils.Rect;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Monde {
    private final World world;
    private final String name;
    private Location spawnPoint;
    private Rect limit = null;
    private final NamedTextColor color;
    private final EWorld type;
    private boolean isLocked;

    public Monde(World world, String name, NamedTextColor color, EWorld type) {
        this.world = world;
        this.name = name;
        this.color = color;
        this.type = type;
    }

    //region Static
    public static Monde get(EWorld worldType) {
        return GameManager.getInstance().getWorldFactory().getWorld(worldType);
    }

    public static Monde get(UUID uuid) {
        return GameManager.getInstance().getWorldFactory().getWorld(uuid);
    }

    public static boolean isPlayerOnWorld(Player player, EWorld world) {
        return player.getWorld().getUID() == get(world).getUUID();
    }

    public static boolean isPlayerOnWorld(Player player, Monde world) {
        return player.getWorld().getUID() == world.getWorld().getUID();
    }

    public static boolean isOutOfLimit(Player player, EWorld world) {
        Block block = player.getLocation().getBlock();
        Monde monde = get(world);
        return isPlayerOnWorld(player, monde) && monde.getLimit() != null && monde.getLimit().isOut(block.getX(), block.getY(), block.getZ());
    }
    //endregion

    //region Getters Setters
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

    public NamedTextColor getColor() {
        return color;
    }

    public EWorld getType() {
        return type;
    }

    public UUID getUUID() {
        return world.getUID();
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLock(boolean lock) {
        this.isLocked = lock;
    }
    //endregion
}
