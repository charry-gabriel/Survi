package fr.miuby.survi.villager;

import fr.miuby.survi.job.EJob;
import fr.miuby.survi.quest.QuestDifficulty;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.List;

public class Trader extends AVillager {
    private final MerchantRecipe[] merchantRecipe;
    private final List<ReputationRecipe> reputationRecipes = new ArrayList<>();

    @Getter
    protected final TextComponent openMessage;

    @Getter
    @Setter
    private QuestDifficulty questDifficulty = QuestDifficulty.COMMON;

    @Getter
    @Setter
    private EJob job = null;

    public Trader(String nameId, TextComponent displayName, Villager.Type type, Villager.Profession profession, MerchantRecipe[] merchantRecipe, TextComponent[] messages, TextComponent openMessage) {
        super(nameId, type, profession, messages);
        this.merchantRecipe = merchantRecipe;
        this.displayName = displayName.color(NamedTextColor.AQUA);
        this.openMessage = openMessage.color(NamedTextColor.WHITE);
        
        for (MerchantRecipe recipe : merchantRecipe) {
            reputationRecipes.add(new ReputationRecipe(recipe, 0));
        }
    }

    public void addReputationRecipe(MerchantRecipe recipe, int requiredReputation) {
        reputationRecipes.add(new ReputationRecipe(recipe, requiredReputation));
    }

    public List<MerchantRecipe> getRecipesForPlayer(int reputation) {
        return reputationRecipes.stream()
                .filter(r -> reputation >= r.requiredReputation())
                .map(ReputationRecipe::recipe)
                .toList();
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

    public record ReputationRecipe(MerchantRecipe recipe, int requiredReputation) {}
}