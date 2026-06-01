package fr.miuby.survi.villager.trader;

import fr.miuby.survi.job.EJob;
import fr.miuby.survi.villager.AVillager;
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

    /**
     * Niveau de difficulté des quêtes proposées par ce Trader (int ≥ 1).
     * 0 = non défini (la difficulté sera tirée aléatoirement selon le niveau du monde).
     */
    @Getter
    @Setter
    private int questDifficulty = 0;

    @Getter
    @Setter
    private EJob job = null;

    @Getter
    @Setter
    private int questCompletionReputation = 0;

    public Trader(String nameId, TextComponent displayName, Villager.Type type, Villager.Profession profession, MerchantRecipe[] merchantRecipe, TextComponent[] messages, TextComponent openMessage) {
        super(nameId, type, profession, messages);
        this.merchantRecipe = merchantRecipe;
        this.displayName = displayName.color(NamedTextColor.AQUA);
        this.openMessage = openMessage.color(NamedTextColor.WHITE);

        for (int i = 0; i < merchantRecipe.length; i++) {
            reputationRecipes.add(new ReputationRecipe(merchantRecipe[i], 0, messages[i]));
        }
    }

    public void addReputationRecipe(MerchantRecipe recipe, int requiredReputation, TextComponent message) {
        reputationRecipes.add(new ReputationRecipe(recipe, requiredReputation, message));
    }

    public List<MerchantRecipe> getRecipesForPlayer(int reputation) {
        return reputationRecipes.stream()
                .filter(r -> reputation >= r.requiredReputation())
                .map(ReputationRecipe::recipe)
                .toList();
    }

    public TextComponent getMessage(ItemStack itemStack) {
        for (ReputationRecipe reputationRecipe : reputationRecipes) {
            if (reputationRecipe.recipe().getResult().isSimilar(itemStack))
                return reputationRecipe.message();
        }
        return Component.text("");
    }

    @Override
    public void createInventory() {
        this.getVillager().setRecipes(getRecipesForPlayer(0));
        this.inventory = this.getVillager().getInventory();
    }

    public record ReputationRecipe(MerchantRecipe recipe, int requiredReputation, TextComponent message) {}
}