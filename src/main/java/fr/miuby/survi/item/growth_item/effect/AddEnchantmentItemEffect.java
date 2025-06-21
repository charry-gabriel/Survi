package fr.miuby.survi.item.growth_item.effect;

import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AddEnchantmentItemEffect implements ItemEffect {
    private final Enchantment enchantment;
    private final int amount;

    public AddEnchantmentItemEffect(Enchantment enchantment, int amount) {
        this.enchantment = enchantment;
        this.amount = amount;
    }

    @Override
    public void apply(ItemStack item, AlphaPlayer player) {
        ItemMeta meta = item.getItemMeta();

        int current = meta.hasEnchant(enchantment) ? meta.getEnchantLevel(enchantment) : 0;
        meta.addEnchant(enchantment, current + amount, true);

        item.setItemMeta(meta);
    }
}
