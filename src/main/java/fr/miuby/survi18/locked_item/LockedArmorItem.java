package fr.miuby.survi18.locked_item;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public class LockedArmorItem extends LockedItem {
    private final LockedArmorType type;

    public LockedArmorItem(List<ItemStack> items, LockedArmorType type) {
        super(items);
        this.type = type;
    }

    public LockedArmorType getType() {
        return type;
    }
}
