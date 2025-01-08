package fr.miuby.survi.item.locked_item;

import org.bukkit.NamespacedKey;

import java.util.List;

public class LockedToolItem extends LockedItem {
    private final LockedToolType type;

    public LockedToolItem(List<NamespacedKey> items, LockedToolType type) {
        super(items);
        this.type = type;
    }

    public LockedToolType getType() {
        return type;
    }
}
