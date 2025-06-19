package fr.miuby.survi.item.locked_item;

import lombok.Getter;
import org.bukkit.NamespacedKey;

import java.util.List;

@Getter
public class LockedItem {
    private final List<NamespacedKey> items;

    private boolean isLocked;

    public LockedItem(List<NamespacedKey> items) {
        this.items = items;
        this.isLocked = true;
    }

    public void unlock() {
        isLocked = false;
    }
}
