package fr.miuby.survi.villager.trader;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.villager.AVillager;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mannequin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class Trader extends AVillager {
    private final List<ReputationRecipe> reputationRecipes = new ArrayList<>();

    @Getter private @Nullable UUID skinUuid;
    @Getter protected TextComponent openMessage;
    @Getter @Setter private fr.miuby.survi.job.EJob job = null;

    public Trader(String nameId, TextComponent displayName, @Nullable UUID skinUuid,
                  MerchantRecipe[] initialRecipes, TextComponent[] messages, TextComponent openMessage) {
        super(nameId, messages);
        this.skinUuid = skinUuid;
        this.displayName = displayName.color(NamedTextColor.AQUA);
        this.openMessage = openMessage.color(NamedTextColor.WHITE);

        for (int i = 0; i < initialRecipes.length; i++) {
            reputationRecipes.add(new ReputationRecipe(initialRecipes[i], 0, messages[i]));
        }
    }

    // =========================================================================
    // EntityType — Mannequin comme VillagerLevel
    // =========================================================================

    @Override
    protected EntityType getEntityType() {
        return EntityType.MANNEQUIN;
    }

    // =========================================================================
    // Skin
    // =========================================================================

    @Override
    protected void onInitialized() {
        if (getVillager() instanceof Mannequin mannequin) {
            mannequin.setImmovable(true);
            mannequin.setDescription(null);
            applySkin(mannequin);
        }
        super.onInitialized();
    }

    private void applySkin(Mannequin mannequin) {
        if (skinUuid == null) return;
        try {
            ResolvableProfile profile = ResolvableProfile.resolvableProfile()
                    .uuid(skinUuid)
                    .build();
            mannequin.setProfile(profile);
        } catch (IllegalArgumentException e) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.VILLAGER,
                    getNameId() + " : skin invalide — UUID attendu, valeur : \"" + skinUuid + "\"");
        }
    }

    // =========================================================================
    // Reload à chaud
    // =========================================================================

    public void reload(TraderConfig config) {
        this.displayName = Component.text(config.displayName).color(NamedTextColor.AQUA);
        this.openMessage = Component.text(config.openMessage).color(NamedTextColor.WHITE);
        this.skinUuid = UUID.fromString(config.skin);
        this.job         = (config.job != null && !config.job.isEmpty())
                ? fr.miuby.survi.job.EJob.valueOf(config.job.toUpperCase()) : null;

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

        if (getVillager() != null) {
            getVillager().customName(displayName);
            if (getVillager() instanceof Mannequin m) applySkin(m);
        }
    }

    // =========================================================================
    // API marchand — recettes calculées à la volée, sans état sur l'entité
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
        for (ReputationRecipe rr : reputationRecipes) {
            if (rr.recipe().getResult().isSimilar(itemStack)) return rr.message();
        }
        return Component.text("");
    }

    /** Pas d'inventaire persistant sur l'entité — le Merchant est créé à la volée dans VillagerListener. */
    @Override
    public void createInventory() {}

    public record ReputationRecipe(MerchantRecipe recipe, int requiredReputation, TextComponent message) {}
}
