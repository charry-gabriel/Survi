package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.item.ECustomItem;
import fr.miuby.survi.player.AlphaPlayer;

public class ItemEffect extends BlessingEffect{
    public final ECustomItem itemStack;

    public ItemEffect(ECustomItem itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        player.getPlayer().getInventory().addItem(itemStack.getItemStack());
    }
}
