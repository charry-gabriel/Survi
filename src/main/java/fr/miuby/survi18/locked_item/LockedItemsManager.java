package fr.miuby.survi18.locked_item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class LockedItemsManager {
    private final List<LockedArmorItem> armorItems = new java.util.ArrayList<>();
    private final List<LockedToolItem> toolItems = new java.util.ArrayList<>();

    public LockedItemsManager() {
        List<ItemStack> leatherItems = new ArrayList<>();
        leatherItems.add(new ItemStack(Material.LEATHER_HELMET));
        leatherItems.add(new ItemStack(Material.LEATHER_CHESTPLATE));
        leatherItems.add(new ItemStack(Material.LEATHER_LEGGINGS));
        leatherItems.add(new ItemStack(Material.LEATHER_BOOTS));
        armorItems.add(new LockedArmorItem(leatherItems, LockedArmorType.LEATHER));

        List<ItemStack> ironItems = new ArrayList<>();
        ironItems.add(new ItemStack(Material.IRON_HELMET));
        ironItems.add(new ItemStack(Material.IRON_CHESTPLATE));
        ironItems.add(new ItemStack(Material.IRON_LEGGINGS));
        ironItems.add(new ItemStack(Material.IRON_BOOTS));
        armorItems.add(new LockedArmorItem(ironItems, LockedArmorType.IRON));
    }

    public boolean isLocked(ItemStack item) {
        for (LockedItem lockedItem : armorItems) {
            if (lockedItem.getItems().contains(item)) {
                return true;
            }
        }
        for (LockedItem lockedItem : toolItems) {
            if (lockedItem.getItems().contains(item)) {
                return true;
            }
        }
        return false;
    }

    public void unlockArmorItem(LockedArmorType itemType) {
        for (LockedArmorItem lockedItem : armorItems) {
            if (lockedItem.getType() == itemType) {
                lockedItem.setLocked(false);
            }
        }
    }

    public void unlockToolItem(LockedToolType itemType) {
        for (LockedToolItem lockedItem : toolItems) {
            if (lockedItem.getType() == itemType) {
                lockedItem.setLocked(false);
            }
        }
    }
}
