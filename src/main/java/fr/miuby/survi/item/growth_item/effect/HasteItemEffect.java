package fr.miuby.survi.item.growth_item.effect;

import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HasteItemEffect implements ItemEffect {
    private final int seconds;

    public HasteItemEffect(int seconds) {
        this.seconds = seconds;
    }

    @Override
    public void apply(ItemStack item, AlphaPlayer player) {
        player.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.HASTE, seconds * 20, 1, false, false, false));
    }
}
