package fr.miuby.survi.villager;

import fr.miuby.lib.villager.MLVillager;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.blessing.Blessing;
import fr.miuby.survi.blessing.BlessingEffect;
import fr.miuby.survi.item.SimpleItemStack;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.trader.Trader;
import fr.miuby.survi.villager.trader.TraderConfig;
import fr.miuby.survi.villager.trader.TraderLoader;
import fr.miuby.survi.villager.villagerlevel.Tribute;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import fr.miuby.survi.villager.villagerlevel.VillagerLevelLoader;
import fr.miuby.survi.blessing.BlessingLoader;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import fr.miuby.survi.job.EJob;
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


    private void addNewTrader(TraderConfig config) {
        Villager.Type type = Registry.VILLAGER_TYPE.get(NamespacedKey.minecraft(config.type.toLowerCase()));
        Villager.Profession profession = Registry.VILLAGER_PROFESSION.get(NamespacedKey.minecraft(config.profession.toLowerCase()));

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

        TextComponent openMessage = Component.text(config.openMessage);

        MLVillager.spawn(() -> {
            Trader trader = new Trader(config.nameId, Component.text(config.displayName), type, profession, recipes, messages, openMessage);
            if (config.job != null && !config.job.isEmpty()) {
                trader.setJob(EJob.valueOf(config.job.toUpperCase()));
            }
            // questDifficulty est maintenant un int : 0 = non défini (aléatoire), ≥1 = niveau fixe
            if (config.questDifficulty > 0) {
                trader.setQuestDifficulty(config.questDifficulty);
            }
            trader.setQuestCompletionReputation(config.questCompletionReputation);

            // Reputations recipes
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
            VillagerPostLoadActions.add(config.nameId, villager ->
                    villager.getVillager().getEquipment().setItemInMainHand(new ItemStack(Material.valueOf(config.mainHandItem.toUpperCase())))
            );
        }
    }

    private void addNewVillagerLevel(String id) {
        VillagerConfig config = VillagerLevelLoader.load(id);

        Villager.Type type = Registry.VILLAGER_TYPE.get(NamespacedKey.minecraft(config.type.toLowerCase()));
        Villager.Profession profession = Registry.VILLAGER_PROFESSION.get(NamespacedKey.minecraft(config.profession.toLowerCase()));
        TextComponent[] names = config.levels.stream().map(level -> Component.text(level.name)).toArray(TextComponent[]::new);
        TextComponent[] messages = config.levels.stream().map(level -> Component.text(level.message)).toArray(TextComponent[]::new);
        TextComponent[] recap = config.levels.stream().map(level -> Component.text(level.recap)).toArray(TextComponent[]::new);
        Tribute[] tributes = config.levels.stream().map(level -> new Tribute(level.tribute.stream().map(SimpleItemStack::toItemStack).toArray(ItemStack[]::new))).toArray(Tribute[]::new);

        Blessing[] blessings = config.levels.stream()
                .map(level -> BlessingLoader.loadFromList(id, level.blessings))
                .map(b -> b != null ? b : new Blessing(new BlessingEffect[0]))
                .toArray(Blessing[]::new);

        Duration[] locks = config.levels.stream()
                .map(level -> level.lock != null ? Duration.ofDays(level.lock) : null)
                .toArray(Duration[]::new);

        MLVillager.spawn(() -> new VillagerLevel(config.name, type, profession, blessings, locks, messages, tributes, names, recap));
    }

    /**
     * Retourne tous les Traders enregistrés dans le VillagerRegistry.
     * Utilisé notamment par AlphaPlayer pour calculer les niveaux de métier.
     */
    public List<Trader> getTraders() {
        return VillagerRegistry.getAll().stream()
                .filter(Trader.class::isInstance)
                .map(v -> (Trader) v)
                .toList();
    }

    public void applyAllCurrentBlessing(AlphaPlayer player) {
        TextComponent.Builder builder = Component.text();

        for (MLVillager villager : VillagerRegistry.getAll()) {
            if (!(villager instanceof VillagerLevel villagerLevel))
                continue;

            villagerLevel.applyAllCurrentBlessing(player);

            Component recap = villagerLevel.getRecapMessage();
            if (recap != null && !PlainTextComponentSerializer.plainText().serialize(recap).isBlank()) {
                builder.append(recap).append(Component.newline());
            }
        }

        Component globalText = builder.build();
        if (PlainTextComponentSerializer.plainText().serialize(globalText).isBlank())
            return;

        player.getPlayer().sendMessage(Component.text()
                .append(Component.text("-------------------- Récapitulatif --------------------\n", NamedTextColor.AQUA))
                .append(globalText)
                .append(Component.text("----------------------------------------------------", NamedTextColor.AQUA))
                .build());
    }
}