package fr.miuby.survi.villager;

import fr.miuby.lib.MLVillagerData;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

@Getter
public class AlphaVillagerData extends MLVillagerData {
    private List<ItemStack> givenItems;
    private int level;

    public AlphaVillagerData(UUID uuid, Location location) {
        super(uuid, location);
    }

    public AlphaVillagerData(UUID uuid, Location location, List<ItemStack> givenItems, int level) {
        super(uuid, location);

        this.givenItems = givenItems;
        this.level = level;
    }
}
