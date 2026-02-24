package fr.miuby.survi.villager;

import fr.miuby.lib.villager.MLVillagerData;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

@Getter
public class AlphaVillagerData extends MLVillagerData {
    private List<ItemStack> givenItems;
    private int level;
    private Long unlockedDate;

    public AlphaVillagerData(UUID uuid, String nameId, Location location) {
        super(uuid, nameId, location);
    }

    public AlphaVillagerData(UUID uuid, String nameId, Location location, List<ItemStack> givenItems, int level, Long unlockedDate) {
        super(uuid, nameId, location);

        this.givenItems = givenItems;
        this.level = level;
        this.unlockedDate = unlockedDate;
    }
}
