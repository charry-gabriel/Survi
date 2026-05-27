package fr.miuby.survi.item.locked_item;

import lombok.Getter;
import org.bukkit.NamespacedKey;

import java.util.List;

@Getter
public class LockedToolItem extends LockedItem {
    private final ELockedToolType type;

    public LockedToolItem(List<NamespacedKey> items, ELockedToolType type) {
        super(items);
        this.type = type;
    }

}
