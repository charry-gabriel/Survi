package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.item.ECustomItem;
import fr.miuby.survi.player.AlphaPlayer;
import lombok.RequiredArgsConstructor;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public class ItemEffect extends BlessingEffect{
    public final ECustomItem itemStack;
    private int amount = 1;

    public ItemEffect(ECustomItem itemStack, int amount) {
        this.itemStack = itemStack;
        this.amount = amount;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        ItemStack itemStack = this.itemStack.getItemStack();
        itemStack.setAmount(this.amount);
        player.getPlayer().getInventory().addItem(itemStack);
    }
}
