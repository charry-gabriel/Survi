package fr.miuby.survi.world;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.utils.Rect;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class Monde {
    private final World world;
    private final String name;
    private final NamedTextColor color;
    private final EWorld type;

    @Setter
    private Location spawnPoint;
    @Setter
    private Rect limit = null;
    @Setter
    private boolean isLocked;

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

    public UUID getUUID() {
        return world.getUID();
    }
}
