package fr.miuby.survi.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class SimpleItemStack {
    public String customItem;
    public String material;
    public int amount = 1;

    public ItemStack toItemStack() {
        if (customItem != null && !customItem.isEmpty()) {
            return ECustomItem.valueOf(customItem).getItemStack(amount);
        }
        return new ItemStack(Material.valueOf(material), amount);
    }
}
