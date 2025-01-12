package fr.miuby.survi.villager;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.utils.Rect;
import fr.miuby.survi.villager.blessing.*;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.item.locked_item.LockedArmorType;
import fr.miuby.survi.item.locked_item.LockedToolType;
import fr.miuby.survi.world.Monde;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nullable;
import java.util.*;

public class VillagerFactory {
    private final World world;
    private final Map<UUID, AVillager> villagers = new HashMap<>();

    public VillagerFactory() {
        this.world = Monde.get(EWorld.VILLAGE).getWorld();

        spawnSurvivant();
        spawnNain();
        spawnMaddox();
        spawnThomas();
        spawnFrancois();

        spawnGolDRoger();
        spawnHermanos();
        spawnSpeedBoots();
        spawnIndiana();
        spawnScaleChestplate();
        spawnBarman();
        spawnReceptionniste();
        spawnConcierge();
    }

    private void addNewVillager(AVillager villager) {
        villagers.put(villager.uuid, villager);
    }

    public Map<UUID, AVillager> getVillagers() {
        return villagers;
    }

    @Nullable
    public AVillager getVillager(String name) {
        for (AVillager villager : villagers.values()) {
            if (name.equals(villager.nameId))
                return villager;
        }
        return null;
    }

    public void applyAllCurrentBlessing(AlphaPlayer player) {
        player.getPlayer().sendMessage(Component.text("-------------------- Récapitulatif --------------------", NamedTextColor.AQUA));
        for (AVillager villager : villagers.values()) {
            if (villager instanceof VillagerLevel villagerLevel) {
                villagerLevel.applyAllCurrentBlessing(player);
                TextComponent text = villagerLevel.getRecapMessage();
                if (!text.content().isEmpty())
                    player.getPlayer().sendMessage(text);
            }
        }
        player.getPlayer().sendMessage(Component.text("----------------------------------------------------", NamedTextColor.AQUA));
    }

    //region Villagers
    private void spawnSurvivant(){
        Tribute[] tributes = new Tribute[]{
                new Tribute(new ItemStack(Material.WHEAT_SEEDS, 32)),
                new Tribute(new ItemStack(Material.OAK_LOG, 640), new ItemStack(Material.APPLE, 32), new ItemStack(Material.SUGAR_CANE, 64)),
                new Tribute(new ItemStack(Material.IRON_BLOCK, 160), new ItemStack(Material.COAL_BLOCK, 64), new ItemStack(Material.LAPIS_BLOCK, 64), new ItemStack(Material.WAXED_COPPER_BLOCK, 16), new ItemStack(Material.WAXED_EXPOSED_COPPER, 16), new ItemStack(Material.WAXED_WEATHERED_COPPER, 16), new ItemStack(Material.WAXED_OXIDIZED_COPPER, 16)),
                new Tribute(new ItemStack(Material.HAY_BLOCK, 32), new ItemStack(Material.BAKED_POTATO, 32), new ItemStack(Material.GOLDEN_CARROT, 128), new ItemStack(Material.PUFFERFISH, 32), new ItemStack(Material.PUMPKIN_PIE, 64)),
                new Tribute(new ItemStack(Material.ENDER_PEARL, 16), new ItemStack(Material.FERMENTED_SPIDER_EYE, 64), new ItemStack(Material.PHANTOM_MEMBRANE, 32), new ItemStack(Material.GUNPOWDER, 64), new ItemStack(Material.BONE, 64), new ItemStack(Material.ROTTEN_FLESH, 64)),
                new Tribute(new ItemStack(Material.ARMADILLO_SCUTE, 10), new ItemStack(Material.PALE_OAK_LOG, 64), new ItemStack(Material.CREAKING_HEART, 64), new ItemStack(Material.RESIN_BLOCK, 64)),
                new Tribute(new ItemStack(Material.OAK_SAPLING, 64), new ItemStack(Material.OAK_LOG, 64), new ItemStack(Material.STRIPPED_OAK_LOG, 64),new ItemStack(Material.SPRUCE_SAPLING, 64), new ItemStack(Material.SPRUCE_LOG, 64), new ItemStack(Material.STRIPPED_SPRUCE_LOG, 64),new ItemStack(Material.BIRCH_SAPLING, 64), new ItemStack(Material.BIRCH_LOG, 64), new ItemStack(Material.STRIPPED_BIRCH_LOG, 64)),
                new Tribute(new ItemStack(Material.JUNGLE_SAPLING, 64), new ItemStack(Material.JUNGLE_LOG, 64), new ItemStack(Material.STRIPPED_JUNGLE_LOG, 64),new ItemStack(Material.ACACIA_SAPLING, 64), new ItemStack(Material.ACACIA_LOG, 64), new ItemStack(Material.STRIPPED_ACACIA_LOG, 64),new ItemStack(Material.DARK_OAK_SAPLING, 64), new ItemStack(Material.DARK_OAK_LOG, 64), new ItemStack(Material.STRIPPED_DARK_OAK_LOG, 64)),
                new Tribute(new ItemStack(Material.MANGROVE_PROPAGULE, 64), new ItemStack(Material.MANGROVE_LOG, 64), new ItemStack(Material.STRIPPED_MANGROVE_LOG, 64),new ItemStack(Material.CHERRY_SAPLING, 64), new ItemStack(Material.CHERRY_LOG, 64), new ItemStack(Material.STRIPPED_CHERRY_LOG, 64),new ItemStack(Material.PALE_OAK_SAPLING, 64), new ItemStack(Material.PALE_OAK_LOG, 64), new ItemStack(Material.STRIPPED_PALE_OAK_LOG, 64)),
                new Tribute(new ItemStack(Material.CHORUS_FRUIT, 64),new ItemStack(Material.GOLDEN_APPLE, 64),new ItemStack(Material.TROPICAL_FISH, 64),new ItemStack(Material.COOKIE, 64),new ItemStack(Material.CAKE, 1)),
                new Tribute(new ItemStack(Material.IRON_BLOCK, 256),new ItemStack(Material.LAPIS_BLOCK, 128),new ItemStack(Material.REDSTONE_BLOCK, 128),new ItemStack(Material.COPPER_BLOCK, 128),new ItemStack(Material.COAL_BLOCK, 128),new ItemStack(Material.GOLD_BLOCK, 64),new ItemStack(Material.DIAMOND_BLOCK, 32),new ItemStack(Material.EMERALD_BLOCK, 16),new ItemStack(Material.NETHERITE_BLOCK, 8)),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new MaxHealthEffect(4),new UnlockToolEffect(LockedToolType.WOOD)),
                new Blessing(new MaxHealthEffect(6),new UnlockArmorEffect(LockedArmorType.LEATHER), new UnlockToolEffect(LockedToolType.STONE)),
                new Blessing(new MaxHealthEffect(8),new UnlockArmorEffect(LockedArmorType.CHAINMAIL), new UnlockToolEffect(LockedToolType.IRON)),
                new Blessing(new MaxHealthEffect(10),new UnlockArmorEffect(LockedArmorType.IRON)),
                new Blessing(new MaxHealthEffect(12),new UnlockArmorEffect(LockedArmorType.GOLD), new UnlockToolEffect(LockedToolType.GOLD)),
                new Blessing(new MaxHealthEffect(14),new UnlockArmorEffect(LockedArmorType.DIAMOND), new UnlockToolEffect(LockedToolType.DIAMOND), new UnlockArmorEffect(LockedArmorType.NETHERITE)),
                new Blessing(new MaxHealthEffect(15)),
                new Blessing(new MaxHealthEffect(16)),
                new Blessing(new MaxHealthEffect(17)),
                new Blessing(new MaxHealthEffect(18)),
                new Blessing(new MaxHealthEffect(20)),
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text("Vous pouvez maintenant avoir des outils en bois !"),
                Component.text( "Vous pouvez maintenant avoir des outils en pierre et une armure en cuir !"),
                Component.text( "Vous pouvez maintenant avoir des outils en fer et une armure en maille !"),
                Component.text( "Vous pouvez maintenant avoir une armure en fer !"),
                Component.text( "Vous pouvez maintenant avoir des outils en or et une armure en or !"),
                Component.text( "Vous pouvez maintenant avoir des outils en diamant et une armure en diamant !"),
                Component.text( "Un peu de vie pour vous !"),
                Component.text( "Un peu de vie pour vous !"),
                Component.text( "Un peu de vie pour vous !"),
                Component.text( "Un peu de vie pour vous !"),
                Component.text( "La quête était si simple en vrai.. fin bref, voici un coeur pour vous."),
                Component.text( "C'est fini y a plus rien !"),

        };

        TextComponent[] recap = new TextComponent[]{
                Component.text( ""),
                Component.text("Outils en bois débloqué !"),
                Component.text( "Outils en bois et armure en cuir débloqué !"),
                Component.text( "Outils en fer et armure en maille débloqué !"),
                Component.text( "Outils en fer et armure en fer débloqué !"),
                Component.text( "Outils en or et armure en or débloqué !"),
                Component.text( "Outils en diamant et armure en diamant débloqué !"),
                Component.text( "Outils en diamant, armure en diamant et vie en plus débloqué !"),
                Component.text( "Outils en diamant, armure en diamant et vie en plus  débloqué !"),
                Component.text( "Outils en diamant, armure en diamant et vie en plus  débloqué !"),
                Component.text( "Outils en diamant, armure en diamant et vie en plus  débloqué !"),
                Component.text( "Outils en diamant, armure en diamant et vie en plus  débloqué !"),

        };

        TextComponent[] names = new TextComponent[]{
                Component.text( "Survivant I"),
                Component.text( "Survivant II"),
                Component.text( "Survivant III"),
                Component.text( "Survivant IV"),
                Component.text( "Survivant V"),
                Component.text( "Survivant VI"),
                Component.text( "Survivant VII"),
                Component.text( "Survivant VIII"),
                Component.text( "Survivant IX"),
                Component.text( "Survivant X"),
                Component.text( "Survivant XI"),
                Component.text( "Survivant MAX"),
        };

        this.addNewVillager(new VillagerLevel("Survivant", Villager.Type.SAVANNA, Villager.Profession.FARMER, blessings, messages, tributes, names, recap));
    }

    private void spawnNain(){
        Tribute[] tributes = new Tribute[]{
                new Tribute(new ItemStack(Material.DIORITE_WALL, 64)),
                new Tribute(new ItemStack(Material.AMETHYST_BLOCK, 128)),
                new Tribute(new ItemStack(Material.RED_NETHER_BRICKS, 192)),
                new Tribute(new ItemStack(Material.SCULK_CATALYST, 256)),
                new Tribute(new ItemStack(Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, 64),new ItemStack(Material.RESIN_BRICK_STAIRS, 32),new ItemStack(Material.DRAGON_BREATH, 1),new ItemStack(Material.WOLF_ARMOR, 1),new ItemStack(Material.RECOVERY_COMPASS, 1),new ItemStack(Material.VERDANT_FROGLIGHT, 20),new ItemStack(Material.OCHRE_FROGLIGHT, 20),new ItemStack(Material.PEARLESCENT_FROGLIGHT, 20)),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new ResistanceEffect(0.4f)),
                new Blessing(new ResistanceEffect(0.6f)),
                new Blessing(new ResistanceEffect(1f)),
                new Blessing(new ResistanceEffect(1.2f)),
                new Blessing(new ResistanceEffect(1.4f)),
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text( "Merci, vouth êteth maintenant pluth résistant face aux ennemith de l'autre monde."),
                Component.text( "Bravo, vouth êteth maintenant pluth résistant face aux ennemith de l'autre monde."),
                Component.text( "Félicitation, vouth êteth maintenant pluth résistant face aux ennemith de l'autre monde."),
                Component.text( "Beau travail, leth ennemith de l'autre monde ne vouth ferronth pluth jamaith de mlin."),
                Component.text( "Félicitation, thèth beau travail, je pense que vouth êteth assez résistant, je peux tidndre ma onatraite. Merci à vouth."),
                Component.text( "Félicitation, thèth beau travail, je pense que vouth êteth assez résistant, je peux tidndre ma onatraite. Merci à vouth."),
        };

        TextComponent[] recap = new TextComponent[]{
                Component.text( ""),
                Component.text( "Résistance I débloqué !"),
                Component.text( "Résistance II débloqué !"),
                Component.text( "Résistance III débloqué !"),
                Component.text( "Résistance IV débloqué !"),
                Component.text( "Résistance V débloqué !"),
        };

        TextComponent[] names = new TextComponent[]{
                Component.text( "Nain Roux I"),
                Component.text( "Nain Roux II"),
                Component.text( "Nain Roux III"),
                Component.text( "Nain Roux IV"),
                Component.text( "Nain Roux V"),
                Component.text( "Nain Roux MAX"),
        };

        this.addNewVillager(new VillagerLevel("Nain", Villager.Type.SWAMP, Villager.Profession.NITWIT, blessings, messages, tributes, names, recap));
    }

    private void spawnMaddox(){
        Tribute[] tributes = new Tribute[]{
                new Tribute(new ItemStack(Material.ARROW, 256)),
                new Tribute(new ItemStack(Material.TNT, 64)),
                new Tribute(new ItemStack(Material.FERMENTED_SPIDER_EYE, 300)),
                new Tribute(new ItemStack(Material.GHAST_TEAR, 64)),
                new Tribute(new ItemStack(Material.DRAGON_HEAD, 10)),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new DamageEffect(0.4f)),
                new Blessing(new DamageEffect(0.6f)),
                new Blessing(new DamageEffect(0.8f)),
                new Blessing(new DamageEffect(1f)),
                new Blessing(new DamageEffect(1.5f)),
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text( "Vous avez tué 3 poulets et vous voulez une récompense ? D'accord.. vous êtes un peu plus fort maintenant mais pas autant que moi quand même."),
                Component.text( "Vous voulez une récompense pour 4 creepers en moins sur la map ? N'importe quoi.. vous êtes un peu plus fort maintenant mais pas autant que moi quand même."),
                Component.text( "Comment on fait un oeil fermenté ? Avec un oeil... et tu le fermentes."),
                Component.text( "Pour les 6 ghasts en moins dans ce monde, je vous offre un peu plus de force."),
                Component.text( "C'est bon ? Vous avez fini de voyager ? Enfin... j'aurais été plus vite tout seul. Vous êtes maintenant assez fort. Mais toujours pas autant que moi."),
                Component.text( "C'est fini, tu tapes beaucoup trop fort maintenant !"),
        };

        TextComponent[] recap = new TextComponent[]{
                Component.text( ""),
                Component.text( "Force I débloqué !"),
                Component.text( "Force II débloqué !"),
                Component.text( "Force III débloqué !"),
                Component.text( "Force IV débloqué !"),
                Component.text( "Force V débloqué !"),
        };

        TextComponent[] names = new TextComponent[]{
                Component.text( "Maddox I"),
                Component.text( "Maddox II"),
                Component.text( "Maddox III"),
                Component.text( "Maddox IV"),
                Component.text( "Maddox V"),
                Component.text( "Maddox MAX"),
        };

        this.addNewVillager(new VillagerLevel("Maddox", Villager.Type.TAIGA, Villager.Profession.BUTCHER, blessings, messages, tributes, names, recap));
    }

    private void spawnThomas(){
        Tribute[] tributes = new Tribute[]{
                new Tribute(new ItemStack(Material.DIRT, 640)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
                new Tribute(new ItemStack(Material.DIAMOND, 32)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
                new Tribute(new ItemStack(Material.WITHER_SKELETON_SKULL, 10)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
                new Tribute(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 3)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
                new Tribute(new ItemStack(Material.DRAGON_EGG, 1)),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new MessageEffect("Niveau I réussi !")),
                new Blessing(new MessageEffect("Niveau II disponible pour Thomas Pesquet !")),
                new Blessing(new LockWorldEffect(EWorld.NETHER), new MessageEffect("Niveau II réussi !")),
                new Blessing(new MessageEffect("Niveau III disponible pour Thomas Pesquet !")),
                new Blessing(new LimitWorldEffect(EWorld.WILDERNESS, new Rect(10000,-10000, Integer.MAX_VALUE, Integer.MIN_VALUE,10000,-10000))),
                new Blessing(new MessageEffect("Niveau VI disponible pour Thomas Pesquet !")),
                new Blessing(new LockWorldEffect(EWorld.END), new MessageEffect("Niveau VI réussi !")),
                new Blessing(new MessageEffect("Niveau V disponible pour Thomas Pesquet !")),
                new Blessing(new LimitWorldEffect(EWorld.WILDERNESS, new Rect(Integer.MAX_VALUE,Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE,Integer.MAX_VALUE,Integer.MIN_VALUE))),
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text( "Merci, revenez dans 24h pour la suite."),
                Component.text( "Venez me voir."),
                Component.text( "Merci, revenez dans 48h pour la suite. Vous pouvez maintenant explorer le nether !"),
                Component.text( "Venez me voir."),
                Component.text( "Merci, revenez dans 72h pour la suite. Vous pouvez maintenant explorer un peu plus le Wilderness"),
                Component.text( "Venez me voir."),
                Component.text( "Merci, revenez dans 100h pour la suite. Vous pouvez maintenant explorer l'end ! "),
                Component.text( "Venez me voir."),
                Component.text( "Merci pour cet objet unique. Vous pouvez maintenant explorer le Wilderness à l'infini."),
                Component.text( "C'est fini y a plus rien !"),
        };

        TextComponent[] recap = new TextComponent[]{
                Component.text( ""),
                Component.text( ""),
                Component.text( ""),
                Component.text( "Nether débloqué !"),
                Component.text( "Nether débloqué !"),
                Component.text( "Nether débloqué et limite Wilderness augmenté !"),
                Component.text( "Nether débloqué et limite Wilderness augmenté !"),
                Component.text( "End débloqué et limite Wilderness augmenté !"),
                Component.text( "End débloqué et limite Wilderness augmenté !"),
                Component.text( "End débloqué et limite Wilderness infinie !"),
        };

        TextComponent[] names = new TextComponent[]{
                Component.text( "Thomas Pesquet I"),
                Component.text( "Thomas Pesquet I"),
                Component.text( "Thomas Pesquet II"),
                Component.text( "Thomas Pesquet II"),
                Component.text( "Thomas Pesquet III"),
                Component.text( "Thomas Pesquet III"),
                Component.text( "Thomas Pesquet IV"),
                Component.text( "Thomas Pesquet IV"),
                Component.text( "Thomas Pesquet V"),
                Component.text( "Thomas Pesquet MAX"),
        };

        this.addNewVillager(new VillagerLevel("Thomas", Villager.Type.SNOW, Villager.Profession.FISHERMAN, blessings, messages, tributes, names, recap));
    }

    private void spawnFrancois(){
        Tribute[] tributes = new Tribute[]{
                new Tribute(new ItemStack(Material.BELL, 1)),
                new Tribute(new ItemStack(Material.CANDLE, 10)),
                new Tribute(new ItemStack(Material.YELLOW_CANDLE, 100)),
                new Tribute(new ItemStack(Material.RED_CANDLE, 200)),
                new Tribute(new ItemStack(Material.ORANGE_CANDLE, 300)),
                new Tribute(new ItemStack(Material.LIGHT_BLUE_CANDLE, 400)),
                new Tribute(new ItemStack(Material.GREEN_CANDLE, 500)),
                new Tribute(new ItemStack(Material.PINK_CANDLE, 600)),
                new Tribute(new ItemStack(Material.WHITE_CANDLE, 700)),
                new Tribute(new ItemStack(Material.TOTEM_OF_UNDYING, 27)),
        };

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

        TextComponent[] messages = new TextComponent[]{
                Component.text( "Vos 10 dernières morts sont annulées."),
                Component.text( "Vos 20 dernières morts sont annulées."),
                Component.text( "Vos 30 dernières morts sont annulées."),
                Component.text( "Vos 40 dernières morts sont annulées."),
                Component.text( "Vos 50 dernières morts sont annulées."),
                Component.text( "Vos 60 dernières morts sont annulées."),
                Component.text( "Vos 70 dernières morts sont annulées."),
                Component.text( "Vos 80 dernières morts sont annulées."),
                Component.text( "Vos 90 dernières morts sont annulées."),
                Component.text( "Toutes vos morts sont annulées"),
                Component.text( "Toutes vos morts sont annulées"),
        };

        TextComponent[] recap = new TextComponent[]{
                Component.text( ""),
                Component.text( "Les 10 dernières morts sont annulées !"),
                Component.text( "Les 20 dernières morts sont annulées !"),
                Component.text( "Les 30 dernières morts sont annulées !"),
                Component.text( "Les 40 dernières morts sont annulées !"),
                Component.text( "Les 50 dernières morts sont annulées !"),
                Component.text( "Les 60 dernières morts sont annulées !"),
                Component.text( "Les 70 dernières morts sont annulées !"),
                Component.text( "Les 80 dernières morts sont annulées !"),
                Component.text( "Les 90 dernières morts sont annulées !"),
                Component.text( "Toutes les morts sont annulées !"),
        };

        TextComponent[] names = new TextComponent[]{
                Component.text( "François I"),
                Component.text( "François II"),
                Component.text( "François III"),
                Component.text( "François IV"),
                Component.text( "François V"),
                Component.text( "François VI"),
                Component.text( "François VII"),
                Component.text( "François VIII"),
                Component.text( "François IX"),
                Component.text( "François X"),
                Component.text( "François MAX"),

        };

        this.addNewVillager(new VillagerLevel("François", Villager.Type.JUNGLE, Villager.Profession.CLERIC, blessings, messages, tributes, names, recap));
    }

    /*private void spawnPecheur1(){
        Location location = new Location(world, 12059.5, 63, 1469.5, 90, 0);

        ItemStack[] items = new ItemStack[]{
                new ItemStack(Material.COOKED_COD, 64),
                new ItemStack(Material.COOKED_SALMON, 64),
                new ItemStack(Material.TROPICAL_FISH, 64),
                new ItemStack(Material.PUFFERFISH, 64),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text( "Merci pour ce poisson, voici 1 émeraude."),
                Component.text( "Merci pour ce poisson, voici 1 émeraude."),
                Component.text( "Merci pour ce poisson, voici 1 émeraude."),
                Component.text( "Merci pour ce poisson, voici 1 émeraude."),
        };

        this.addNewVillager(new VillagerVendor("Vendeur Bajau", location, Villager.Type.SWAMP, Villager.Profession.NITWIT, blessings, messages, items));
    }

    private void spawnPecheur2(){
        Location location = new Location(world, 12045, 63, 1454.5, -90, 0);

        ItemStack[] items = new ItemStack[]{
                new ItemStack(Material.NAUTILUS_SHELL, 64),
                new ItemStack(Material.NAME_TAG, 64),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 2))),
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text( "Merci pour ces coquillages, voici 1 émeraude."),
                Component.text( "Merci pour ces names-tags, voici 2 émeraude."),
        };

        this.addNewVillager(new VillagerVendor("Vendeur Sampan",location, Villager.Type.SWAMP, Villager.Profession.NITWIT, blessings, messages, items));
    }

    private void spawnRiche(){
        Location location = new Location(world, 12072.5, 64, 1483.5, -90, 0);

        Tribute[] tributes = new Tribute[]{
                new Tribute(new ItemStack(Material.COPPER_BLOCK, 64), new ItemStack(Material.WAXED_COPPER_BLOCK, 64), new ItemStack(Material.EXPOSED_COPPER, 64), new ItemStack(Material.WAXED_EXPOSED_COPPER, 64), new ItemStack(Material.WEATHERED_COPPER, 64), new ItemStack(Material.WAXED_WEATHERED_COPPER, 64), new ItemStack(Material.OXIDIZED_COPPER, 64), new ItemStack(Material.WAXED_OXIDIZED_COPPER, 64), new ItemStack(Material.TORCHFLOWER, 64), new ItemStack(Material.PITCHER_PLANT, 64)),
                new Tribute(new ItemStack(Material.COAL, 128), new ItemStack(Material.CHARCOAL, 128), new ItemStack(Material.COPPER_INGOT, 128), new ItemStack(Material.IRON_INGOT, 256), new ItemStack(Material.GOLD_INGOT, 128), new ItemStack(Material.REDSTONE_BLOCK, 64), new ItemStack(Material.QUARTZ_BLOCK, 64), new ItemStack(Material.DIAMOND, 64), new ItemStack(Material.EMERALD, 16), new ItemStack(Material.FLINT, 128), new ItemStack(Material.GLOWSTONE_DUST, 128), new ItemStack(Material.NETHERITE_INGOT, 6)),
                new Tribute(new ItemStack(Material.STRIPPED_OAK_WOOD, 64), new ItemStack(Material.STRIPPED_SPRUCE_WOOD, 64), new ItemStack(Material.STRIPPED_BIRCH_WOOD, 64), new ItemStack(Material.STRIPPED_JUNGLE_WOOD, 64), new ItemStack(Material.STRIPPED_ACACIA_WOOD, 64), new ItemStack(Material.STRIPPED_DARK_OAK_WOOD, 64), new ItemStack(Material.STRIPPED_MANGROVE_WOOD, 64), new ItemStack(Material.STRIPPED_CHERRY_WOOD, 64), new ItemStack(Material.STRIPPED_BAMBOO_BLOCK, 64), new ItemStack(Material.STRIPPED_WARPED_HYPHAE, 64), new ItemStack(Material.STRIPPED_CRIMSON_HYPHAE, 64)),
                new Tribute(new ItemStack(Material.SPYGLASS, 1), new ItemStack(Material.CLOCK, 1), new ItemStack(Material.SHEARS, 1), new ItemStack(Material.FLINT_AND_STEEL, 1), new ItemStack(Material.BRUSH, 1), new ItemStack(Material.LEAD, 64), new ItemStack(Material.COMPASS, 64), new ItemStack(Material.RECOVERY_COMPASS, 9), new ItemStack(Material.MUSIC_DISC_CAT, 10)),
                new Tribute(new ItemStack(Material.TURTLE_HELMET, 3), new ItemStack(Material.LEATHER_HORSE_ARMOR, 3), new ItemStack(Material.IRON_HORSE_ARMOR, 3), new ItemStack(Material.GOLDEN_HORSE_ARMOR, 3), new ItemStack(Material.DIAMOND_HORSE_ARMOR, 3), new ItemStack(Material.RESPAWN_ANCHOR, 128)),
                new Tribute(new ItemStack(Material.NETHER_WART, 640), new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 10), new ItemStack(Material.NETHER_STAR, 10), new ItemStack(Material.NETHERITE_INGOT, 640), new ItemStack(Material.WITHER_ROSE, 3), new ItemStack(Material.WITHER_SKELETON_SKULL, 30)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new MessageEffect("IMPOSSIBLE")),
                new Blessing(new MessageEffect("IMPOSSIBLE")),
                new Blessing(new MessageEffect("IMPOSSIBLE")),
                new Blessing(new MessageEffect("IMPOSSIBLE")),
                new Blessing(new MessageEffect("IMPOSSIBLE")),
                new Blessing(new MessageEffect("IMPOSSIBLE")),
                new Blessing(new MessageEffect("IMPOSSIBLE")),
                new Blessing(new MessageEffect("IMPOSSIBLE")),
                new Blessing(new MessageEffect("IMPOSSIBLE")),
                new Blessing(new MessageEffect("IMPOSSIBLE")),
                new Blessing(new MessageEffect("IMPOSSIBLE")),
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text( "Thomas Pesquet sera plus efficace d'une heure !"),
                Component.text( "Thomas Pesquet sera plus efficace de deux heures !"),
                Component.text( "Thomas Pesquet sera plus efficace de trois heures !"),
                Component.text( "Thomas Pesquet sera plus efficace de cinq heures !"),
                Component.text( "Thomas Pesquet sera plus efficace de sept heures !"),
                Component.text( "Thomas Pesquet sera plus efficace de neuf heures !"),
                Component.text( "Thomas Pesquet sera plus efficace de douze heures !"),
                Component.text( "Thomas Pesquet sera plus efficace de quinze heures !"),
                Component.text( "Thomas Pesquet sera plus efficace de dix-huit heures !"),
                Component.text( "Thomas Pesquet sera plus efficace de vingt-huit heures !"),
                Component.text( "IMPOSSIBLE"),
        };

        TextComponent[] names = new TextComponent[]{
                Component.text( "Ariscis I"),
                Component.text( "Cidrouille I"),
                Component.text( "flyzeur_ I"),
                Component.text( "Ariscis II"),
                Component.text( "Cidrouille II"),
                Component.text( "flyzeur_ II"),
                Component.text( "Ariscis III"),
                Component.text( "Cidrouille III"),
                Component.text( "flyzeur_ III"),
                Component.text( "Riche MAX"),
                Component.text( "IMPOSSIBLE"),
        };

        this.addNewVillager(new VillagerLevel("Ariscis I", location, Villager.Type.TAIGA, Villager.Profession.CLERIC, blessings, messages, tributes, names));
    }

    private void spawnFermier1(){
        Location location = new Location(world, 12010.5, 64, 1464.5, -90, 0);

        ItemStack[] items = new ItemStack[]{
                new ItemStack(Material.DRIED_KELP, 64),
                new ItemStack(Material.COOKED_BEEF, 64),
                new ItemStack(Material.COOKED_PORKCHOP, 64),
                new ItemStack(Material.COOKED_MUTTON, 64),
                new ItemStack(Material.COOKED_CHICKEN, 64),
                new ItemStack(Material.COOKED_RABBIT, 64),
                new ItemStack(Material.RABBIT_FOOT, 64),
                new ItemStack(Material.LEATHER, 64),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 2))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text( "Merci pour ces algues, voici 1 émeraude."),
                Component.text( "Merci pour la viande, voici 1 émeraude."),
                Component.text( "Merci pour la viande, voici 1 émeraude."),
                Component.text( "Merci pour la viande, voici 1 émeraude."),
                Component.text( "Merci pour la viande, voici 1 émeraude."),
                Component.text( "Merci pour la viande, voici 1 émeraude."),
                Component.text( "Merci pour ces pattes, voici 2 émeraude."),
                Component.text( "Merci pour ce cuir, voici 1 émeraude."),
        };

        this.addNewVillager(new VillagerVendor("Vendeur Santoku", location, Villager.Type.SWAMP, Villager.Profession.NITWIT, blessings, messages, items));
    }

    private void spawnFermier2(){
        Location location = new Location(world, 12010.5, 64, 1466.5, -90, 0);

        ItemStack[] items = new ItemStack[]{
                new ItemStack(Material.HAY_BLOCK, 64),
                new ItemStack(Material.GOLDEN_CARROT, 64),
                new ItemStack(Material.BAKED_POTATO, 64),
                new ItemStack(Material.PUMPKIN_PIE, 64),
                new ItemStack(Material.MELON, 64),
                new ItemStack(Material.NETHER_WART_BLOCK, 64),
                new ItemStack(Material.BEETROOT, 64),
                new ItemStack(Material.GLOW_BERRIES, 64),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 2))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 2))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 2))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text( "Merci pour ce blé, voici 2 émeraude."),
                Component.text( "Merci pour ces carottes, voici 2 émeraude."),
                Component.text( "Merci pour ces patates, voici 1 émeraude."),
                Component.text( "Merci pour ces tartes, voici 2 émeraude."),
                Component.text( "Merci pour ce melon, voici 1 émeraude."),
                Component.text( "Merci pour ces verrues, voici 1 émeraude."),
                Component.text( "Merci pour ces betteraves, voici 1 émeraude."),
                Component.text( "Merci pour ces baies, voici 1 émeraude."),
        };

        this.addNewVillager(new VillagerVendor("Vendeur Tanaka", location, Villager.Type.SWAMP, Villager.Profession.NITWIT, blessings, messages, items));
    }

    private void spawnPharmacien(){
        Location location = new Location(world, 12035.5, 64, 1425.5, 0, 0);

        ItemStack[] items = new ItemStack[]{
                new ItemStack(Material.EMERALD_BLOCK, 1),
        };

        ItemStack itemStack = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) itemStack.getItemMeta();
        meta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH,1,1), false);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.ABSORPTION,3600*20,4), false);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED,20*20,20), false);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.REGENERATION,10*20,10), false);
        itemStack.setItemMeta(meta);

        Blessing[] blessings = new Blessing[]{
                new Blessing(new ItemEffect(itemStack)),
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text( "Voici un médicament, tu iras mieux avec ça !"),
        };

        this.addNewVillager(new VillagerVendor("Vendeur Nossos", location, Villager.Type.DESERT, Villager.Profession.CLERIC, blessings, messages, items));
    }

    private void spawnPolicier(){
        Location location = new Location(world, 12068.5, 67, 1402.5, 90, 0);

        ItemStack[] items = new ItemStack[]{
                new ItemStack(Material.EMERALD_BLOCK, 1),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new GameModeEffect(GameMode.SURVIVAL)),
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text( "Vous êtes libre, mais on vous surveille."),
        };

        this.addNewVillager(new VillagerVendor("Vendeur Aspis", location, Villager.Type.SNOW, Villager.Profession.FLETCHER, blessings, messages, items));
    }*/

    private void spawnGolDRoger(){
        ItemStack[] items = new ItemStack[]{
                new ItemStack(Material.EMERALD_BLOCK, 9),
        };

        ItemStack itemStack = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = itemStack.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 5,true);
        meta.addEnchant(Enchantment.LURE, 5,true);
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 5,true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1,false);
        itemStack.setItemMeta(meta);

        Blessing[] blessings = new Blessing[]{
                new Blessing(new ItemEffect(itemStack)),
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text("Voici une canne à pêche !"),
        };

        TextComponent openMessage = Component.text("Veux-tu une belle canne à pêche ?");

        AVillager villager = new VillagerVendor("GoldRoger", Component.text("Gol D. Roger"), Villager.Type.SAVANNA, Villager.Profession.WEAPONSMITH, blessings, messages, items, openMessage);
        villager.getVillager().getEquipment().setItemInMainHand(new ItemStack(Material.FISHING_ROD));
        this.addNewVillager(villager);
    }

    private void spawnHermanos(){
        ItemStack[] items = new ItemStack[]{
                new ItemStack(Material.CHICKEN_SPAWN_EGG, 1),
        };

        ItemStack itemStack = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemStack.getItemMeta();
        meta.addStoredEnchant(Enchantment.MENDING, 1,false);
        itemStack.setItemMeta(meta);

        Blessing[] blessings = new Blessing[]{
                new Blessing(new ItemEffect(itemStack)),
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text( "Voici un livre rare, utilise le intelligemment."),
        };

        TextComponent openMessage = Component.text("J'adore le poulet. Je ne vends aucune drogue.");

        this.addNewVillager(new VillagerVendor("Hermanos", Component.text("Los Pollos Hermanos"), Villager.Type.SWAMP, Villager.Profession.LIBRARIAN, blessings, messages, items, openMessage));
    }

    private void spawnSpeedBoots(){
        ItemStack[] items = new ItemStack[]{
                new ItemStack(Material.CHICKEN_SPAWN_EGG, 1),
        };

        ItemStack itemStack = new ItemStack(Material.LEATHER_BOOTS);

        ArmorMeta armorMeta = (ArmorMeta) itemStack.getItemMeta();
        armorMeta.setTrim(new ArmorTrim(TrimMaterial.NETHERITE, TrimPattern.SILENCE));
        itemStack.setItemMeta(armorMeta);

        LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta) itemStack.getItemMeta();
        leatherArmorMeta.setColor(Color.fromRGB(16383998));
        itemStack.setItemMeta(leatherArmorMeta);

        ItemMeta meta = itemStack.getItemMeta();
        meta.addAttributeModifier(Attribute.MOVEMENT_SPEED,
                new AttributeModifier(new NamespacedKey(GameManager.getInstance().getPlugin(), "AirForceSpeed"),
                        0.1f,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.FEET));
        meta.addAttributeModifier(Attribute.BLOCK_BREAK_SPEED,
                new AttributeModifier(new NamespacedKey(GameManager.getInstance().getPlugin(), "AirForceBlockBreakSpeed"),
                        -0.8f,
                        AttributeModifier.Operation.ADD_SCALAR,
                        EquipmentSlotGroup.FEET));
        meta.addAttributeModifier(Attribute.ARMOR,
                new AttributeModifier(new NamespacedKey(GameManager.getInstance().getPlugin(), "AirForceArmor"),
                        -0.8f,
                        AttributeModifier.Operation.ADD_SCALAR,
                        EquipmentSlotGroup.FEET));
        meta.setUnbreakable(true);
        meta.customName(Component.text("Air Force 1", NamedTextColor.YELLOW));
        meta.addItemFlags(ItemFlag.HIDE_DYE);
        meta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
        itemStack.setItemMeta(meta);

        Blessing[] blessings = new Blessing[]{
                new Blessing(new ItemEffect(itemStack)),
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text("Voici une paire d'Air Force 1."),
        };

        TextComponent openMessage = Component.text("Les Air Force 1, les chaussures qui courent vite.");

        this.addNewVillager(new VillagerVendor("Nike_49", Component.text("Nike_49"), Villager.Type.PLAINS, Villager.Profession.LEATHERWORKER, blessings, messages, items, openMessage));
    }

    private void spawnIndiana(){
        ItemStack[] items = new ItemStack[]{
                new ItemStack(Material.CHICKEN_SPAWN_EGG, 1),
        };

        ItemStack itemStack = new ItemStack(Material.LEATHER_HELMET);

        ArmorMeta armorMeta = (ArmorMeta) itemStack.getItemMeta();
        armorMeta.setTrim(new ArmorTrim(TrimMaterial.GOLD, TrimPattern.FLOW));
        itemStack.setItemMeta(armorMeta);

        LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta) itemStack.getItemMeta();
        leatherArmorMeta.setColor(Color.fromRGB(13061821));
        itemStack.setItemMeta(leatherArmorMeta);

        ItemMeta meta = itemStack.getItemMeta();
        meta.addAttributeModifier(Attribute.MINING_EFFICIENCY,
                new AttributeModifier(new NamespacedKey(GameManager.getInstance().getPlugin(), "CasqueDeMineurMining"),
                        10f,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.HEAD));
        meta.addAttributeModifier(Attribute.MOVEMENT_SPEED,
                new AttributeModifier(new NamespacedKey(GameManager.getInstance().getPlugin(), "CasqueDeMineurSpeed"),
                        -0.02f,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.HEAD));
        meta.addAttributeModifier(Attribute.ARMOR,
                new AttributeModifier(new NamespacedKey(GameManager.getInstance().getPlugin(), "CasqueDeMineurArmor"),
                        -0.8f,
                        AttributeModifier.Operation.ADD_SCALAR,
                        EquipmentSlotGroup.HEAD));
        meta.setUnbreakable(true);
        meta.customName(Component.text("Casque de Mineur", NamedTextColor.YELLOW));
        meta.addItemFlags(ItemFlag.HIDE_DYE);
        meta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
        itemStack.setItemMeta(meta);

        Blessing[] blessings = new Blessing[]{
                new Blessing(new ItemEffect(itemStack)),
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text("Voici un Casque de Mineur"),
        };

        TextComponent openMessage = Component.text("Le Casque de Mineur, le casque qui mine vite.");

        this.addNewVillager(new VillagerVendor("Indiana", Component.text("Indiana"), Villager.Type.SNOW, Villager.Profession.CARTOGRAPHER, blessings, messages, items, openMessage));
    }

    private void spawnScaleChestplate(){
        ItemStack[] items = new ItemStack[]{
                new ItemStack(Material.CHICKEN_SPAWN_EGG, 1),
        };

        ItemStack itemStack = new ItemStack(Material.LEATHER_CHESTPLATE);

        ArmorMeta armorMeta = (ArmorMeta) itemStack.getItemMeta();
        armorMeta.setTrim(new ArmorTrim(TrimMaterial.AMETHYST, TrimPattern.SILENCE));
        itemStack.setItemMeta(armorMeta);

        LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta) itemStack.getItemMeta();
        leatherArmorMeta.setColor(Color.fromRGB(1408423));
        itemStack.setItemMeta(leatherArmorMeta);

        ItemMeta meta = itemStack.getItemMeta();
        meta.addAttributeModifier(Attribute.SCALE,
                new AttributeModifier(new NamespacedKey(GameManager.getInstance().getPlugin(), "CombinaisonEndialeScale"),
                        -0.5f,
                        AttributeModifier.Operation.ADD_SCALAR,
                        EquipmentSlotGroup.CHEST));
        meta.addAttributeModifier(Attribute.BLOCK_BREAK_SPEED,
                new AttributeModifier(new NamespacedKey(GameManager.getInstance().getPlugin(), "CombinaisonEndialeBlockBreakSpeed"),
                        -0.9f,
                        AttributeModifier.Operation.ADD_SCALAR,
                        EquipmentSlotGroup.CHEST));
        meta.addAttributeModifier(Attribute.MOVEMENT_SPEED,
                new AttributeModifier(new NamespacedKey(GameManager.getInstance().getPlugin(), "CombinaisonEndialeSpeed"),
                        -0.02f,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.CHEST));
        meta.setUnbreakable(true);
        meta.customName(Component.text("Combinaison Endiale", NamedTextColor.YELLOW));
        meta.addItemFlags(ItemFlag.HIDE_DYE);
        meta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
        itemStack.setItemMeta(meta);

        Blessing[] blessings = new Blessing[]{
                new Blessing(new ItemEffect(itemStack)),
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text("Voici une Combinaison pour visiter l'End"),
        };

        TextComponent openMessage = Component.text("La Combinaison Endiale, la combinaison à la bonne taille !");

        this.addNewVillager(new VillagerVendor("Sophie", Component.text("Sophie Adenot"), Villager.Type.SWAMP, Villager.Profession.FISHERMAN, blessings, messages, items, openMessage));
    }

    private void spawnBarman(){
        ItemStack[] items = new ItemStack[]{
                new ItemStack(Material.GOLD_INGOT, 10),
                new ItemStack(Material.DIAMOND, 4),
                new ItemStack(Material.GOLD_BLOCK, 8),
                new ItemStack(Material.EMERALD, 1),
        };

        ItemStack itemStack = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) itemStack.getItemMeta();
        meta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED,900*20,4), false);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,900*20,1), false);
        itemStack.setItemMeta(meta);

        ItemStack itemStack2 = new ItemStack(Material.POTION);
        PotionMeta meta2 = (PotionMeta) itemStack2.getItemMeta();
        meta2.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH,1,3), false);
        meta2.addCustomEffect(new PotionEffect(PotionEffectType.REGENERATION,120*20,1), false);
        itemStack2.setItemMeta(meta2);

        ItemStack itemStack3 = new ItemStack(Material.POTION);
        PotionMeta meta3 = (PotionMeta) itemStack3.getItemMeta();
        meta3.addCustomEffect(new PotionEffect(PotionEffectType.STRENGTH,300*20,2), false);
        meta3.addCustomEffect(new PotionEffect(PotionEffectType.WATER_BREATHING,300*20,1), false);
        itemStack3.setItemMeta(meta3);

        ItemStack itemStack4 = new ItemStack(Material.NAME_TAG);

        Blessing[] blessings = new Blessing[]{
                new Blessing(new ItemEffect(itemStack)),
                new Blessing(new ItemEffect(itemStack2)),
                new Blessing(new ItemEffect(itemStack3)),
                new Blessing(new ItemEffect(itemStack4)),
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text("Très bon choix! Rendez-vous à minuit au casino au premier étage pour y admirer ces délicieuses peintures. Les dés sont lancés, puisse le sort et votre rencontre vous être favorables: votre regard sur votre voisin de la chambre 32, ou votre impression sur votre voisine du 2ième risque d’être chamboulés …"),
                Component.text("Très bon choix! Rendez-vous à minuit au casino au premier étage pour y admirer ces délicieuses peintures. Les dés sont lancés, puisse le sort et votre rencontre vous être favorables: votre regard sur votre voisin de la chambre 32, ou votre impression sur votre voisine du 2ième risque d’être chamboulés …"),
                Component.text("Très bon choix! Rendez-vous à minuit au casino au premier étage pour y admirer ces délicieuses peintures. Les dés sont lancés, puisse le sort et votre rencontre vous être favorables: votre regard sur votre voisin de la chambre 32, ou votre impression sur votre voisine du 2ième risque d’être chamboulés …"),
                Component.text("Très bon choix! Rendez-vous à minuit au casino au premier étage pour y admirer ces délicieuses peintures. Les dés sont lancés, puisse le sort et votre rencontre vous être favorables: votre regard sur votre voisin de la chambre 32, ou votre impression sur votre voisine du 2ième risque d’être chamboulés …"),
        };

        TextComponent openMessage = Component.text("Bonjour, bienvenue au bar du Merveilleux Royal Bling-Bling Sexy-Baka Palace-Hôtel. A la carte, nous proposons différents cocktails élaborés avec amour, tendresse et voluptuosité: le Sex On the Beach, le Porn Star Martini, et notre fameux Shooter Orgasm. D’autre part, je peux aussi proposer un Spicy Sweet Dreams Ticket si vous le désirez !");

        AVillager villager = new VillagerVendor("Barman", Component.text("Fruity Délice"), Villager.Type.SAVANNA, Villager.Profession.FLETCHER, blessings, messages, items, openMessage);
        villager.getVillager().getEquipment().setItemInMainHand(new ItemStack(Material.POTION));
        this.addNewVillager(villager);
    }

    private void spawnReceptionniste(){
        ItemStack[] items = new ItemStack[]{
                new ItemStack(Material.BEDROCK, 9),
        };

        ItemStack itemStack = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = itemStack.getItemMeta();
        meta.getPersistentDataContainer().set(new NamespacedKey(GameManager.getInstance().getPlugin(), "unique_id"), PersistentDataType.STRING, "la cle 1");
        itemStack.setItemMeta(meta);

        Blessing[] blessings = new Blessing[]{
                new Blessing(new ItemEffect(itemStack)),
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text("Merci pour votre confiance. Vous ne regretterez pas votre séjour dans notre hôtel au concept inédit."),
        };

        TextComponent openMessage = Component.text("La réception, c’est ma vocation. Bienvenue au Merveilleux Royal Bling-Bling Sexy-Baka Palace-Hôtel. Je vous accueille à bras ouverts dans notre somptueux manoir du 19ième, où le service 5 étoiles fourni par nos employés sauront ravir toutes vos exigences. Le discrétion c’est notre crédo… profitez de votre séjour et explorez à votre rythme chaque recoin de notre manoir qui abrite bien plus de secrets que vous ne pouvez l’imaginer. \n" +
                "Si vous souhaitez siroter une délicieux cocktail aphrodisiaque, notre barman Fruity Delice saura vous contenter. En cas de demande particulière, veuillez-vous référez à Jean Touchatouille… mais on ne sait jamais où il traine celui-ci, il sait se faire discret. Jacques Black, notre croupier, est au premier étage si une envie de jouer vous vient.De toute manière, ici tous les jeux sont gagnants! ");

        AVillager villager = new VillagerVendor("Receptionniste", Component.text("Alainse Lapince"), Villager.Type.JUNGLE, Villager.Profession.CARTOGRAPHER, blessings, messages, items, openMessage);
        this.addNewVillager(villager);
    }

    private void spawnConcierge() {
        ItemStack cle = new ItemStack(Material.FISHING_ROD);
        ItemMeta metaCle = cle.getItemMeta();
        metaCle.getPersistentDataContainer().set(new NamespacedKey(GameManager.getInstance().getPlugin(), "unique_id"), PersistentDataType.STRING, "la cle 1");
        cle.setItemMeta(metaCle);

        ItemStack[] items = new ItemStack[]{
                cle,
        };

        ItemStack itemStack = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = itemStack.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 5,true);
        meta.addEnchant(Enchantment.LURE, 5,true);
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 5,true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1,false);
        itemStack.setItemMeta(meta);

        Blessing[] blessings = new Blessing[]{
                new Blessing(new ItemEffect(itemStack)),
        };

        TextComponent[] messages = new TextComponent[]{
                Component.text("Allez, dégagez maintenant, je dois terminer mon nettoyage !"),
        };

        TextComponent openMessage = Component.text("Quoi… Que… Que… vous m’avez repéré… ce n’est pas ce que vous croyez, je… je faisais le ménage! En tout cas j’ai pu être spectateur d’une performance honorable de votre part. Le shooter Orgasme de notre barman Fruity Delice a réussi son effet! Eum Eum… \n" + "\n" +
                "Ecoutez, je pense qu’on peut s’arranger, je ne voudrais pas que de fausses rumeurs se répandent… que diriez-vous d’un arrangement? On est presque ami maintenant. Confirmez-moi que vous êtes bien client de l’hôtel, fournissez-moi vos clefs, et je vous fournirai une récompense en échange de votre aimable discrétion et de votre départ précipité.");

        AVillager villager = new VillagerVendor("Concierge", Component.text("Jean Touchatouille"), Villager.Type.TAIGA, Villager.Profession.ARMORER, blessings, messages, items, openMessage);
        this.addNewVillager(villager);
    }


    /*private void spawnLibraire(){
        Location location = new Location(world, 12138.5, 73, 1447.5, -135, 0);

        ItemStack itemStack = new ItemStack(Material.ENCHANTED_BOOK, 1);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemStack.getItemMeta();
        meta.addStoredEnchant(Enchantment.UNBREAKING, 3,false);
        itemStack.setItemMeta(meta);

        ItemStack itemStack2 = new ItemStack(Material.ENCHANTED_BOOK, 1);
        EnchantmentStorageMeta meta2 = (EnchantmentStorageMeta) itemStack2.getItemMeta();
        meta2.addStoredEnchant(Enchantment.PROTECTION, 4,false);
        itemStack2.setItemMeta(meta2);

        ItemStack itemStack3 = new ItemStack(Material.ENCHANTED_BOOK, 1);
        EnchantmentStorageMeta meta3 = (EnchantmentStorageMeta) itemStack3.getItemMeta();
        meta3.addStoredEnchant(Enchantment.EFFICIENCY, 5,false);
        itemStack3.setItemMeta(meta3);

        ItemStack itemStack4 = new ItemStack(Material.ENCHANTED_BOOK, 1);
        EnchantmentStorageMeta meta4 = (EnchantmentStorageMeta) itemStack4.getItemMeta();
        meta4.addStoredEnchant(Enchantment.THORNS, 3,false);
        itemStack4.setItemMeta(meta4);

        ItemStack itemStack5 = new ItemStack(Material.ENCHANTED_BOOK, 1);
        EnchantmentStorageMeta meta5 = (EnchantmentStorageMeta) itemStack5.getItemMeta();
        meta5.addStoredEnchant(Enchantment.PUNCH, 2,false);
        itemStack5.setItemMeta(meta5);

        ItemStack[] items = new ItemStack[]{
                itemStack, itemStack2, itemStack3, itemStack4, itemStack5
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 9))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 9))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 9))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 9))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 9))),

        };

        TextComponent[] messages = new TextComponent[]{
                Component.text( "Merci pour ce livre, voici un peu d'émeraude."),
                Component.text( "Merci pour ce livre, voici un peu d'émeraude."),
                Component.text( "Merci pour ce livre, voici un peu d'émeraude."),
                Component.text( "Merci pour ce livre, voici un peu d'émeraude."),
                Component.text( "Merci pour ce livre, voici un peu d'émeraude."),
        };

        this.addNewVillager(new VillagerVendor("Junkudo", location, Villager.Type.SAVANNA, Villager.Profession.LIBRARIAN, blessings, messages, items));
    }*/
    //endregion
}
