package fr.miuby.survi.world.crops;

import lombok.Getter;
import org.bukkit.Location;

@Getter
public class PlantedCrop {
    private final String worldUid;
    private final int x;
    private final int y;
    private final int z;

    public PlantedCrop(Location location) {
        this.worldUid = location.getWorld().getUID().toString();
        this.x = location.getBlockX();
        this.y = location.getBlockY();
        this.z = location.getBlockZ();
    }

    public PlantedCrop(String worldUid, int x, int y, int z) {
        this.worldUid = worldUid;
        this.x = x;
        this.y = y;
        this.z = z;
    }


    public String getKey() {
        return String.format("%s;%d;%d;%d", worldUid, x, y, z);
    }


    public static PlantedCrop fromKey(String key) {
        String[] parts = key.split(";");
        return new PlantedCrop(parts[0], 
            Integer.parseInt(parts[1]), 
            Integer.parseInt(parts[2]), 
            Integer.parseInt(parts[3]));
    }
}
