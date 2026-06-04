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
    protected TextComponent openMessage;

    @Getter @Setter
    private EJob job = null;

    public Trader(String nameId, TextComponent displayName, Villager.Type type, Villager.Profession profession,
                  MerchantRecipe[] merchantRecipe, TextComponent[] messages, TextComponent openMessage) {
        super(nameId, type, profession, messages);
        this.merchantRecipe = merchantRecipe;
        this.displayName    = displayName.color(NamedTextColor.AQUA);
        this.openMessage    = openMessage.color(NamedTextColor.WHITE);

        for (int i = 0; i < merchantRecipe.length; i++) {
            reputationRecipes.add(new ReputationRecipe(merchantRecipe[i], 0, messages[i]));
        }
    }

    // =========================================================================
    // Reload à chaud
    // =========================================================================

    /**
     * Recharge la configuration de ce Trader depuis le {@link TraderConfig} fourni,
     * sans recréer l'entité Bukkit.
     *
     * <h3>Ce qui change immédiatement en jeu</h3>
     * <ul>
     *   <li>Nametag du villageois (displayName).</li>
     *   <li>Recettes de commerce (accessible aux joueurs qui ré-ouvrent le menu).</li>
     *   <li>Message d'ouverture et messages par recette.</li>
     *   <li>Métier (EJob) pour les récompenses de réputation.</li>
     * </ul>
     *
     * <p>Les joueurs qui ont actuellement le menu de commerce ouvert voient les changements
     * à leur prochaine ouverture (Bukkit ne permet pas la mise à jour à chaud d'un MerchantInventory déjà ouvert).</p>
     */
    public void reload(TraderConfig config) {
        this.displayName = Component.text(config.displayName).color(NamedTextColor.AQUA);
        this.openMessage = Component.text(config.openMessage).color(NamedTextColor.WHITE);
        this.job         = (config.job != null && !config.job.isEmpty()) ? EJob.valueOf(config.job.toUpperCase()) : null;

        // Mise à jour des messages pour les recettes de base (réputation = 0)
        TextComponent[] newMessages = config.recipes.stream()
                .filter(r -> r.requiredReputation <= 0)
                .map(r -> Component.text(r.message))
                .toArray(TextComponent[]::new);
        this.messages = newMessages;

        // Reconstruction complète des recettes (base + réputation)
        reputationRecipes.clear();
        config.recipes.stream()
                .filter(r -> r.requiredReputation <= 0)
                .forEach(r -> {
                    MerchantRecipe recipe = new MerchantRecipe(r.result.toItemStack(), 0, 99, false, 0, 0, 9, 0, true);
                    recipe.addIngredient(r.ingredient.toItemStack());
                    reputationRecipes.add(new ReputationRecipe(recipe, 0, Component.text(r.message)));
                });
        config.recipes.stream()
                .filter(r -> r.requiredReputation > 0)
                .forEach(r -> {
                    MerchantRecipe recipe = new MerchantRecipe(r.result.toItemStack(), 0, 99, false, 0, 0, 9, 0, true);
                    recipe.addIngredient(r.ingredient.toItemStack());
                    reputationRecipes.add(new ReputationRecipe(recipe, r.requiredReputation, Component.text(r.message)));
                });

        // Applique sur l'entité villageois en jeu
        if (getVillager() != null) {
            getVillager().customName(displayName);
            getVillager().setRecipes(getRecipesForPlayer(0));
        }
    }

    // =========================================================================
    // API publique
    // =========================================================================

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