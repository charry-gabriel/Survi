package fr.miuby.survi18.locked_item;

import org.bukkit.inventory.ItemStack;
import java.util.List;

public class LockedItem {
    private final LockedItemType type;
    private final List<ItemStack> items;

    public LockedItem(LockedItemType type, List<ItemStack> items) {
        this.type = type;
        this.items = items;
    }

    public LockedItemType getType() {
        return type;
    }

    public List<ItemStack> getItems() {
        return items;
    }
}
