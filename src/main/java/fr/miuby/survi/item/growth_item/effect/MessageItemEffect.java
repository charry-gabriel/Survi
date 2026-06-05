package fr.miuby.survi.item.growth_item.effect;

import fr.miuby.survi.player.AlphaPlayer;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;

public record MessageItemEffect(TextComponent message) implements ItemEffect {

    @Override
    public void apply(ItemStack item, AlphaPlayer player) {
        player.getPlayer().sendMessage(message.colorIfAbsent(NamedTextColor.GREEN));
    }

    @Override
    public boolean isTransient() { return true; }
}