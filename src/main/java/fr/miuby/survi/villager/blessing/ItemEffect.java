package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.item.ECustomItem;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.VillagerLevel;
import lombok.RequiredArgsConstructor;
import org.bukkit.inventory.ItemStack;

@RequiredArgsConstructor
public class ItemEffect extends BlessingEffect{
    private final ECustomItem itemStack;
    private final int amount = 1;

    @Override
    public void applyEffect(VillagerLevel villager, AlphaPlayer player) {
        ItemStack itemStack = this.itemStack.getItemStack();
        itemStack.setAmount(this.amount);
        player.getPlayer().getInventory().addItem(itemStack);
    }
}
