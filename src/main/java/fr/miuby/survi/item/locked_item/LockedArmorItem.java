package fr.miuby.survi.item.locked_item;

import lombok.Getter;
import org.bukkit.NamespacedKey;

import java.util.List;

@Getter
public class LockedArmorItem extends LockedItem {
    private final ELockedArmorType type;

    public LockedArmorItem(List<NamespacedKey> items, ELockedArmorType type) {
        super(items);
        this.type = type;
    }

}
