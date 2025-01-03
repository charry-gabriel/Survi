package fr.miuby.survi.item.locked_item;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public class LockedToolItem extends LockedItem {
    private final LockedToolType type;

    public LockedToolItem(List<ItemStack> items, LockedToolType type) {
        super(items);
        this.type = type;
    }

    public LockedToolType getType() {
        return type;
    }
}
