package fr.miuby.survi.item.locked_item;

import lombok.Getter;
import org.bukkit.NamespacedKey;

import java.util.List;

@Getter
public class LockedArmorItem extends LockedItem {
    private final LockedArmorType type;

    public LockedArmorItem(List<NamespacedKey> items, LockedArmorType type) {
        super(items);
        this.type = type;
    }

}
