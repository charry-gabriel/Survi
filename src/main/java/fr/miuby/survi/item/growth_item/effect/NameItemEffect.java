package fr.miuby.survi.item.growth_item.effect;

import fr.miuby.survi.player.AlphaPlayer;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public record NameItemEffect(String name) implements ItemEffect {

    @Override
    public void apply(ItemStack item, AlphaPlayer player) {
        ItemMeta meta = item.getItemMeta();
        if (meta.customName() instanceof TextComponent current) {
            meta.customName(current.content(name));
        }
        item.setItemMeta(meta);
    }
}
