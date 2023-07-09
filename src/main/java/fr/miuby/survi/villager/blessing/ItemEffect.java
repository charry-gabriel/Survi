package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.AlphaPlayer;
import org.bukkit.inventory.ItemStack;

public class ItemEffect extends BlessingEffect{
    public final ItemStack itemStack;

    public ItemEffect(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        player.getPlayer().getInventory().addItem(itemStack);
    }
}
