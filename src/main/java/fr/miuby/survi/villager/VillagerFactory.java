package fr.miuby.survi.villager;

import fr.miuby.lib.villager.MLVillager;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.lib.utils.Rect;
import fr.miuby.survi.item.SimpleItemStack;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.quest.QuestDifficulty;
import fr.miuby.survi.villager.trader.Trader;
import fr.miuby.survi.villager.trader.TraderConfig;
import fr.miuby.survi.villager.trader.TraderLoader;
import fr.miuby.survi.villager.villagerlevel.Tribute;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import fr.miuby.survi.villager.villagerlevel.VillagerLevelLoader;
import fr.miuby.survi.villager.villagerlevel.blessing.*;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.item.locked_item.LockedArmorType;
import fr.miuby.survi.item.locked_item.LockedToolType;
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
        spawnSurvivant();
        spawnNain();
        spawnMaddox();
        spawnThomas();
        spawnFrancois();

        TraderLoader.loadAll().forEach(this::addNewTrader);
    }

    //region VillagerLevel
    private void spawnSurvivant(){
        Blessing[] blessings = new Blessing[]{
                new Blessing(new MaxHealthEffect(-20)),
                new Blessing(new MaxHealthEffect(-18),new UnlockToolEffect(LockedToolType.WOOD)),
                new Blessing(new MaxHealthEffect(-16),new UnlockArmorEffect(LockedArmorType.LEATHER), new UnlockToolEffect(LockedToolType.STONE)),
                new Blessing(new MaxHealthEffect(-14),new UnlockArmorEffect(LockedArmorType.CHAINMAIL), new UnlockToolEffect(LockedToolType.IRON)),
                new Blessing(new MaxHealthEffect(-12),new UnlockArmorEffect(LockedArmorType.IRON)),
                new Blessing(new MaxHealthEffect(-10),new UnlockArmorEffect(LockedArmorType.GOLD), new UnlockToolEffect(LockedToolType.GOLD)),
                new Blessing(new MaxHealthEffect(-8),new UnlockArmorEffect(LockedArmorType.DIAMOND), new UnlockToolEffect(LockedToolType.DIAMOND), new UnlockArmorEffect(LockedArmorType.NETHERITE)),
                new Blessing(new MaxHealthEffect(-6)),
                new Blessing(new MaxHealthEffect(-4)),
                new Blessing(new MaxHealthEffect(-2)),
                new Blessing(new MaxHealthEffect(0)),
                new Blessing(new MaxHealthEffect(2)),
        };

        this.addNewVillagerLevel("survivant", blessings);
    }

    private void spawnNain(){
        Blessing[] blessings = new Blessing[]{
                new Blessing(new ResistanceEffect(0.4f)),
                new Blessing(new ResistanceEffect(0.6f)),
                new Blessing(new ResistanceEffect(1f)),
                new Blessing(new ResistanceEffect(1.2f)),
                new Blessing(new ResistanceEffect(1.4f)),
        };

        this.addNewVillagerLevel("nain", blessings);
    }

    private void spawnMaddox(){
        Blessing[] blessings = new Blessing[]{
                new Blessing(new DamageEffect(0.4f)),
                new Blessing(new DamageEffect(0.6f)),
                new Blessing(new DamageEffect(0.8f)),
                new Blessing(new DamageEffect(1f)),
                new Blessing(new DamageEffect(1.5f)),
        };

        this.addNewVillagerLevel("maddox", blessings);
    }

    private void spawnThomas(){
        String villagerId = "thomas";
        Blessing[] blessings = new Blessing[]{
                new Blessing(new LockVillagerEffect(Duration.ofDays(3)), new MessageEffect("Niveau I réussi !")),
                new Blessing(new LockVillagerEffect(Duration.ofDays(3)), new MessageEffect("Niveau II disponible pour Thomas Pesquet !")),
                new Blessing(new LockWorldEffect(EWorld.NETHER), new MessageEffect("Niveau II réussi !")),
                new Blessing(new LockVillagerEffect(Duration.ofDays(3)), new MessageEffect("Niveau III disponible pour Thomas Pesquet !")),
                new Blessing(new LimitWorldEffect(EWorld.WILDERNESS, new Rect(10000,-10000, Integer.MAX_VALUE, Integer.MIN_VALUE,10000,-10000))),
                new Blessing(new LockVillagerEffect(Duration.ofDays(3)), new MessageEffect("Niveau VI disponible pour Thomas Pesquet !")),
                new Blessing(new LockWorldEffect(EWorld.END), new MessageEffect("Niveau VI réussi !")),
                new Blessing(new LockVillagerEffect(Duration.ofDays(3)), new MessageEffect("Niveau V disponible pour Thomas Pesquet !")),
                new Blessing(new LimitWorldEffect(EWorld.WILDERNESS, new Rect(Integer.MAX_VALUE,Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE,Integer.MAX_VALUE,Integer.MIN_VALUE))),
        };

        this.addNewVillagerLevel(villagerId, blessings);
    }

    private void spawnFrancois(){
        Blessing[] blessings = new Blessing[]{
                new Blessing(new DispelEffect(1)),
                new Blessing(new DispelEffect(2)),
                new Blessing(new DispelEffect(3)),
                new Blessing(new DispelEffect(4)),
                new Blessing(new DispelEffect(5)),
                new Blessing(new DispelEffect(6)),
                new Blessing(new DispelEffect(7)),
                new Blessing(new DispelEffect(8)),
                new Blessing(new DispelEffect(9)),
                new Blessing(new DispelEffect(100)),
        };

        this.addNewVillagerLevel("francois", blessings);
    }
    //endregion VillagerLevel


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
            if (config.questDifficulty != null && !config.questDifficulty.isEmpty()) {
                trader.setQuestDifficulty(QuestDifficulty.valueOf(config.questDifficulty.toUpperCase()));
            }

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

    private void addNewVillagerLevel(String id, Blessing[] blessings) {
        VillagerConfig config = VillagerLevelLoader.load(id);

        Villager.Type type = Registry.VILLAGER_TYPE.get(NamespacedKey.minecraft(config.type.toLowerCase()));
        Villager.Profession profession = Registry.VILLAGER_PROFESSION.get(NamespacedKey.minecraft(config.profession.toLowerCase()));
        TextComponent[] names = config.levels.stream().map(level -> Component.text(level.name)).toArray(TextComponent[]::new);
        TextComponent[] messages = config.levels.stream().map(level -> Component.text(level.message)).toArray(TextComponent[]::new);
        TextComponent[] recap = config.levels.stream().map(level -> Component.text(level.recap)).toArray(TextComponent[]::new);
        Tribute[] tributes = config.levels.stream().map(level -> new Tribute(level.tribute.stream().map(SimpleItemStack::toItemStack).toArray(ItemStack[]::new))).toArray(Tribute[]::new);

        MLVillager.spawn(() -> new VillagerLevel(config.name, type, profession, blessings, messages, tributes, names, recap));
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

            villagerLevel.applyAllCurrentBlessing(villagerLevel, player);

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