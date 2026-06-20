package fr.miuby.survi.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import lombok.RequiredArgsConstructor;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public class ItemEffect extends BlessingEffect {
    private final ItemStack itemStack;
    private final int amount;

    @Override
    public void applyEffect(AlphaPlayer player) {
        ItemStack item = this.itemStack.clone();
        item.setAmount(this.amount);
        player.getPlayer().getInventory().addItem(item);
    }

    @Override
    public boolean requiresOnlinePlayer() { return true; }
}