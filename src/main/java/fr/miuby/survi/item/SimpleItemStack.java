package fr.miuby.survi.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class SimpleItemStack {
    public String material;
    public int amount;

    public ItemStack toItemStack() {
        return new ItemStack(Material.valueOf(material), amount);
    }
}
