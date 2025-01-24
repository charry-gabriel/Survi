package fr.miuby.survi.villager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.List;

public class Trader extends AVillager {
    private final MerchantRecipe[] merchantRecipe;
    private final TextComponent displayName;

    public Trader(String nameId, TextComponent displayName, Villager.Type type, Villager.Profession profession, MerchantRecipe[] merchantRecipe, TextComponent[] messages, TextComponent openMessage) {
        super(nameId, type, profession, messages, openMessage);
        this.merchantRecipe = merchantRecipe;
        this.nameId = nameId;
        this.displayName = displayName;

        initVillager();
        getVillager().customName(getDisplayName());
        createInventory();
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


    @Override
    public TextComponent getDisplayName() {
        return displayName.color(NamedTextColor.AQUA);
    }
}
