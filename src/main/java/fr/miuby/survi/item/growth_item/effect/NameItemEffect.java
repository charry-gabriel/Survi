package fr.miuby.survi.item.growth_item.effect;

import fr.miuby.survi.player.AlphaPlayer;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public record NameItemEffect(TextComponent name) implements ItemEffect {

    @Override
    public void apply(ItemStack item, AlphaPlayer player) {
        ItemMeta meta = item.getItemMeta();

        meta.customName(name.colorIfAbsent(NamedTextColor.GOLD));

        item.setItemMeta(meta);
    }
}
