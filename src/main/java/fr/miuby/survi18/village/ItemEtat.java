package fr.miuby.survi18.village;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ItemEtat {
    private ItemStack item;
    private int price;
    private boolean onSale;
    private ItemStack[] itemStack;

    public ItemEtat(Material material, int price, boolean onSale, ItemStack... itemStack) {
        this.price = price;
        this.onSale = onSale;
        this.itemStack = itemStack;

        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        List<Component> lore = new ArrayList<>();
        if(price > 0)
            lore.add(Component.text(price + " AlphaCoins"));
        for(int loop = 0; loop < itemStack.length; loop++) {
            lore.add(Component.text(itemStack[loop].getAmount() + " " + itemStack[loop].getType().toString()));
        }
        meta.lore(lore);
        stack.setItemMeta(meta);
        item = stack;
    }

    public ItemEtat(ItemStack item, int price, boolean onSale, ItemStack... itemStack) {
        this.price = price;
        this.onSale = onSale;
        this.itemStack = itemStack;

        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        if(price > 0)
            lore.add(Component.text(price + " AlphaCoins"));
        for(int loop = 0; loop < itemStack.length; loop++) {
            lore.add(Component.text(itemStack[loop].getAmount() + " " + itemStack[loop].getType().toString()));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        this.item = item;
    }

    public ItemStack getItem() {
        return item;
    }

    public int getPrice() {
        return price;
    }

    public boolean isOnSale() {
        return onSale;
    }

    public ItemStack[] getItemStack() {
        return itemStack;
    }
}
