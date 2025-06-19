package fr.miuby.survi.item.locked_item;

import lombok.Getter;
import org.bukkit.NamespacedKey;

import java.util.List;

@Getter
public class LockedToolItem extends LockedItem {
    private final LockedToolType type;

    public LockedToolItem(List<NamespacedKey> items, LockedToolType type) {
        super(items);
        this.type = type;
    }

}
