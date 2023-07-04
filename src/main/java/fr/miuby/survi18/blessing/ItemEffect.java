package fr.miuby.survi18.blessing;

import fr.miuby.survi18.AlphaPlayer;
import org.bukkit.inventory.ItemStack;

public class ItemEffect extends BlessingEffect{
    public ItemStack itemStack;

    public ItemEffect(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        player.getPlayer().getInventory().addItem(itemStack);
    }
}
