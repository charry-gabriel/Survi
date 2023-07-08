package fr.miuby.survi.locked_item;

import org.bukkit.inventory.ItemStack;
import java.util.List;

public class LockedItem {
    private final List<ItemStack> items;

    private boolean isLocked;

    public LockedItem(List<ItemStack> items) {
        this.items = items;
        this.isLocked = true;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }
}
