package fr.miuby.survi.item.growth_item.effect;

import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.inventory.ItemStack;

@FunctionalInterface
public interface ItemEffect {
    void apply(ItemStack item, AlphaPlayer player);
}
