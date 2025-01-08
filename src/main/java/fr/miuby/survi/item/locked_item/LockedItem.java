package fr.miuby.survi.item.locked_item;

import org.bukkit.NamespacedKey;

import java.util.List;

public class LockedItem {
    private final List<NamespacedKey> items;

    private boolean isLocked;

    public LockedItem(List<NamespacedKey> items) {
        this.items = items;
        this.isLocked = true;
    }

    public List<NamespacedKey> getItems() {
        return items;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void unlock() {
        isLocked = false;
    }
}
