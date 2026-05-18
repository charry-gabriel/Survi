package fr.miuby.survi.villager.villagerlevel;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@Getter
public class Tribute {
    private final List<ItemStack> itemStacks;

    public Tribute(ItemStack... itemStacks) {
        this.itemStacks = new LinkedList<>(Arrays.asList(itemStacks));
    }

}
