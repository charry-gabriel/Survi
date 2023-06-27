package fr.miuby.survi18.locked_item;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public class LockedItemsManager {
    private List<LockedItem> items = new java.util.ArrayList<LockedItem>();

    public boolean isLocked(ItemStack item) {
        for (LockedItem lockedItem : items) {
            if (lockedItem.getItems().contains(item)) {
                return true;
            }
        }
        return false;
    }
}
