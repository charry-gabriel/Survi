package fr.miuby.survi;

import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Tribute {
    private final List<ItemStack> itemStacks;

    public Tribute(ItemStack... itemStacks) {
        this.itemStacks = new LinkedList<>(Arrays.asList(itemStacks));
    }

    public List<ItemStack> getItemStacks() {
        return itemStacks;
    }
}
