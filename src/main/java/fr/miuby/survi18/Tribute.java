package fr.miuby.survi18;

import org.bukkit.inventory.ItemStack;

public class Tribute {
    private final ItemStack[] itemStacks;

    public Tribute(ItemStack... itemStacks) {
        this.itemStacks = itemStacks;
    }

    public ItemStack[] getItemStacks() {
        return itemStacks;
    }
}
