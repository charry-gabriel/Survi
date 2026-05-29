package fr.miuby.survi.blessing;

import fr.miuby.survi.item.ECustomItem;
import fr.miuby.survi.player.AlphaPlayer;
import lombok.RequiredArgsConstructor;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public class ItemEffect extends BlessingEffect{
    private final ECustomItem itemStack;
    private final int amount = 1;

    @Override
    public void applyEffect(AlphaPlayer player) {
        ItemStack item = this.itemStack.getItemStack();
        item.setAmount(this.amount);
        player.getPlayer().getInventory().addItem(item);
    }
}
