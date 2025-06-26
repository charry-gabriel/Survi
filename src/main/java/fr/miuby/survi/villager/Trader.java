package fr.miuby.survi.villager;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.List;

public class Trader extends AVillager {
    private final MerchantRecipe[] merchantRecipe;
    @Getter
    protected final TextComponent openMessage;

    public Trader(String nameId, TextComponent displayName, Villager.Type type, Villager.Profession profession, MerchantRecipe[] merchantRecipe, TextComponent[] messages, TextComponent openMessage) {
        super(nameId, type, profession, messages);
        this.merchantRecipe = merchantRecipe;
        this.displayName = displayName.color(NamedTextColor.AQUA);
        this.openMessage = openMessage;
    }

    public TextComponent getMessage(ItemStack itemStack) {
        for (int loop = 0; loop < merchantRecipe.length; loop++) {
            if (merchantRecipe[loop].getResult().isSimilar(itemStack))
                return messages[loop];
        }
        return Component.text("");
    }

    @Override
    public void createInventory() {
        this.getVillager().setRecipes(List.of(merchantRecipe));
        this.inventory = this.getVillager().getInventory();
    }
}
