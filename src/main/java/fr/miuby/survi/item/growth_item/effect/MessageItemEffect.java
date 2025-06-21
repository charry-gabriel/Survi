package fr.miuby.survi.item.growth_item.effect;

import fr.miuby.survi.player.AlphaPlayer;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;

public class MessageItemEffect implements ItemEffect {
    private final TextComponent message;

    /// color GREEN if not defined
    public MessageItemEffect(TextComponent message) {
        this.message = message;
    }

    @Override
    public void apply(ItemStack item, AlphaPlayer player) {
        player.getPlayer().sendMessage(message.colorIfAbsent(NamedTextColor.GREEN));
    }
}
