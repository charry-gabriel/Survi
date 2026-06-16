package fr.miuby.survi.villager;

import fr.miuby.lib.resource.MLResourceManager;
import fr.miuby.lib.villager.MLVillager;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.blessing.Blessing;
import fr.miuby.survi.blessing.BlessingEffect;
import fr.miuby.survi.blessing.BlessingLoader;
import fr.miuby.survi.item.SimpleItemStack;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.villager.trader.Trader;
import fr.miuby.survi.villager.trader.TraderConfig;
import fr.miuby.survi.villager.trader.TraderLoader;
import fr.miuby.survi.villager.villagerlevel.Tribute;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import fr.miuby.survi.villager.villagerlevel.VillagerLevelLoader;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.time.Duration;
import java.util.List;

@Getter
public class VillagerFactory {

    public VillagerFactory() {
        addNewVillagerLevel("survivant");
        addNewVillagerLevel("nain");
        addNewVillagerLevel("maddox");
        addNewVillagerLevel("thomas");
        addNewVillagerLevel("francois");

        TraderLoader.loadAll().forEach(this::addNewTrader);
    }

    // =========================================================================
    // Reload à chaud
    // =========================================================================

    public void reloadAll() {
        MLResourceManager.clearCache();

        for (MLVillager villager : VillagerRegistry.getAll()) {
            if (villager instanceof VillagerLevel vl) {
                VillagerConfig config = VillagerLevelLoader.load(vl.getNameId());
                if (config != null) vl.reloadConfig(config);
            } else if (villager instanceof Trader trader) {
                TraderConfig config = TraderLoader.load(trader.getNameId());
                if (config != null) trader.reload(config);
            }
        }
    }

    // =========================================================================
    // Création — Trader
    // =========================================================================

    private void addNewTrader(TraderConfig config) {
        MerchantRecipe[] recipes = config.recipes.stream()
                .filter(r -> r.requiredReputation <= 0)
                .map(r -> {
                    MerchantRecipe recipe = new MerchantRecipe(r.result.toItemStack(), 0, 99, false, 0, 0, 9, 0, true);
                    recipe.addIngredient(r.ingredient.toItemStack());
                    return recipe;
                }).toArray(MerchantRecipe[]::new);

        TextComponent[] messages = config.recipes.stream()
                .filter(r -> r.requiredReputation <= 0)
                .map(r -> Component.text(r.message))
                .toArray(TextComponent[]::new);

        MLVillager.spawn(() -> {
            Trader trader = new Trader(
                    config.nameId,
                    Component.text(config.displayName),
                    config.skin,
                    recipes,
                    messages,
                    Component.text(config.openMessage)
            );

            if (config.job != null && !config.job.isEmpty())
                trader.setJob(EJob.valueOf(config.job.toUpperCase()));

            config.recipes.stream()
                    .filter(r -> r.requiredReputation > 0)
                    .forEach(r -> {
                        MerchantRecipe recipe = new MerchantRecipe(r.result.toItemStack(), 0, 99, false, 0, 0, 9, 0, true);
                        recipe.addIngredient(r.ingredient.toItemStack());
                        trader.addReputationRecipe(recipe, r.requiredReputation, Component.text(r.message));
                    });

            return trader;
        });

        if (config.mainHandItem != null && !config.mainHandItem.isEmpty()) {
            VillagerPostLoadActions.add(config.nameId, v ->
                    v.getVillager().getEquipment().setItemInMainHand(
                            new ItemStack(Material.valueOf(config.mainHandItem.toUpperCase()))
                    )
            );
        }
    }

    // =========================================================================
    // Création — VillagerLevel
    // =========================================================================

    private void addNewVillagerLevel(String id) {
        VillagerConfig config = VillagerLevelLoader.load(id);

        TextComponent[] names    = config.levels.stream().map(l -> Component.text(l.name)).toArray(TextComponent[]::new);
        TextComponent[] messages = config.levels.stream().map(l -> Component.text(l.message)).toArray(TextComponent[]::new);
        TextComponent[] recap    = config.levels.stream().map(l -> Component.text(l.recap)).toArray(TextComponent[]::new);
        Tribute[] tributes       = config.levels.stream()
                .map(l -> new Tribute(l.tribute.stream().map(SimpleItemStack::toItemStack).toArray(ItemStack[]::new)))
                .toArray(Tribute[]::new);
        Blessing[] blessings     = config.levels.stream()
                .map(l -> BlessingLoader.loadFromList(id, l.blessings))
                .map(b -> b != null ? b : new Blessing(new BlessingEffect[0]))
                .toArray(Blessing[]::new);
        Duration[] locks         = config.levels.stream()
                .map(l -> l.lock != null ? Duration.ofDays(l.lock) : null)
                .toArray(Duration[]::new);

        MLVillager.spawn(() -> new VillagerLevel(config.name, config.skin, blessings, locks, messages, tributes, names, recap));
    }

    // =========================================================================
    // Accesseurs
    // =========================================================================

    public List<Trader> getTraders() {
        return VillagerRegistry.getAll().stream()
                .filter(Trader.class::isInstance)
                .map(v -> (Trader) v)
                .toList();
    }
}
