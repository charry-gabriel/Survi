package fr.miuby.survi.item.locked_item;

import org.bukkit.NamespacedKey;

import java.util.List;

public class LockedArmorItem extends LockedItem {
    private final LockedArmorType type;

    public LockedArmorItem(List<NamespacedKey> items, LockedArmorType type) {
        super(items);
        this.type = type;
    }

    public LockedArmorType getType() {
        return type;
    }
}
