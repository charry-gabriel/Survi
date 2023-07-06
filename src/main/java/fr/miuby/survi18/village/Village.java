package fr.miuby.survi18.village;

import fr.miuby.survi18.blessing.*;
import fr.miuby.survi18.Tribute;
import fr.miuby.survi18.locked_item.LockedArmorType;
import fr.miuby.survi18.locked_item.LockedToolType;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class Village {
    private final World world;
    private final Map<String, VillagerLevel> villagersLevel = new HashMap<>();
    private final Map<String, VillagerEtat> villagersEtat = new HashMap<>();
    private final Map<String, VillagerVendor> villagersVendor = new HashMap<>();

    public Village(World world) {
        this.world = world;

        spawnEdward();
        spawnNain();
        spawnMaddox();
        spawnThomas();
        spawnStuff();
        spawnTools();
        spawnFrancois();
        spawnHeros();
        spawnPecheur1();
        spawnPecheur2();
        spawnFermier1();
        spawnFermier2();
        spawnRiche();
        spawnGolDRoger();
    }

    public World getWorld() { return world; }

    public Map<String, VillagerLevel> getVillagersLevel() {
        return villagersLevel;
    }

    public Map<String, VillagerVendor> getVillagersVendor() {
        return villagersVendor;
    }

    private void spawnEdward(){
        Location location = new Location(world, 12012.5, 64, 1465.5, -90, 0);

        Tribute[] tributes = new Tribute[]{
                new Tribute(new ItemStack(Material.HAY_BLOCK, 64)),
                new Tribute(new ItemStack(Material.GOLDEN_CARROT, 128)),
                new Tribute(new ItemStack(Material.BAKED_POTATO, 192)),
                new Tribute(new ItemStack(Material.PUMPKIN_PIE, 256)),
                new Tribute(new ItemStack(Material.SUGAR, 320)),
                new Tribute(new ItemStack(Material.MELON, 384)),
                new Tribute(new ItemStack(Material.GREEN_DYE, 448)),
                new Tribute(new ItemStack(Material.COOKIE, 4096)),
                new Tribute(new ItemStack(Material.TROPICAL_FISH, 666)),
                new Tribute(new ItemStack(Material.NETHER_WART_BLOCK, 100)),
                new Tribute(new ItemStack(Material.NETHER_STAR, 100)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new MaxHealthEffect(12)),
                new Blessing(new MaxHealthEffect(14)),
                new Blessing(new MaxHealthEffect(16)),
                new Blessing(new MaxHealthEffect(18)),
                new Blessing(new MaxHealthEffect(20)),
                new Blessing(new MaxHealthEffect(24)),
                new Blessing(new MaxHealthEffect(28)),
                new Blessing(new MaxHealthEffect(32)),
                new Blessing(new MaxHealthEffect(34)),
                new Blessing(new MaxHealthEffect(35)),
                new Blessing(new RegenEffect()),
                new Blessing(new MessageEffect("IMPOSSIBLE")),

        };

        Component[] messages = new Component[]{
                Component.text("Merci beaucoup pour cette nourriture. Tenez, 1 coeur pour vous !"),
                Component.text( "Merci merci. Pourquoi autant ? C'est pas grave. Tenez, 1 coeur de plus pour vous."),
                Component.text( "J'adore les patates ! Merci ! Je vous offre ce coeur"),
                Component.text( "Merci. Prenez ce coeur, je n'ai pas le temps de vous parler."),
                Component.text( "Bravo ! J'avais tellement besoin de ce sucre. Je vous prépare une surprise mais j'ai besoin d'autre ingrédients. Tenez ce coeur, vous en aurez sûrement plus besoin que moi."),
                Component.text( "Du melon, c'est parfait, c'est exactement dont ce que j'avais besoin ! Vous voici maintenant avec 2 coeurs supplémentaires."),
                Component.text( "C'est uniquement pour la couleur de la soupe que j'ai demandée du cactus, je suppose que vous avez fait ça vite de toute façon. Prenez ces 2 coeurs en plus quand même."),
                Component.text( "Félicitation, vous voici maintenant à 32 HP."),
                Component.text( "Félicitation, vous voici maintenant à 34 HP. J'ai presque fini la potion magique."),
                Component.text( "Bravo, vous avez maintenant 35 HP. Il me manque que quelques étoiles du nether et je suis prêt. Allez me le chercher, vite vite vite !!!!"),
                Component.text( "Félicitations, j'ai fini !! Vous pouvez regagner votre vie sans pomme !! AHAHAH !! Je suis le meilleur !!"),
                Component.text( "IMPOSSIBLE"),

        };

        Component[] names = new Component[]{
                Component.text( "Edward Jenner I"),
                Component.text( "Edward Jenner II"),
                Component.text( "Edward Jenner III"),
                Component.text( "Edward Jenner IV"),
                Component.text( "Edward Jenner V"),
                Component.text( "Edward Jenner VI"),
                Component.text( "Edward Jenner VII"),
                Component.text( "Edward Jenner VIII"),
                Component.text( "Edward Jenner IX"),
                Component.text( "Edward Jenner X"),
                Component.text( "Edward Jenner MAX"),
                Component.text( "Edward Jenner MAX"),
        };

        VillagerLevel villager = new VillagerLevel(location,"Edward Jenner I", Villager.Type.SAVANNA, Villager.Profession.FARMER, blessings, messages, tributes, names);
        villagersLevel.put("Edward Jenner I", villager);
    }

    private void spawnNain(){
        Location location = new Location(world, 12076.5, 64, 1445.5, 90, 0);

        Tribute[] tributes = new Tribute[]{
                new Tribute(new ItemStack(Material.DIORITE_WALL, 64)),
                new Tribute(new ItemStack(Material.AMETHYST_BLOCK, 128)),
                new Tribute(new ItemStack(Material.RED_NETHER_BRICKS, 192)),
                new Tribute(new ItemStack(Material.SCULK_CATALYST, 256)),
                new Tribute(new ItemStack(Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, 320)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new ResistanceEffect(0.4f)),
                new Blessing(new ResistanceEffect(0.6f)),
                new Blessing(new ResistanceEffect(1f)),
                new Blessing(new ResistanceEffect(1.2f)),
                new Blessing(new ResistanceEffect(1.4f)),
                new Blessing(new MessageEffect("IMPOSSIBLE")),
        };

        Component[] messages = new Component[]{
                Component.text( "Merci, vouth êteth maintenant pluth résistant face aux ennemith de l'autre monde."),
                Component.text( "Bravo, vouth êteth maintenant pluth résistant face aux ennemith de l'autre monde."),
                Component.text( "Félicitation, vouth êteth maintenant pluth résistant face aux ennemith de l'autre monde."),
                Component.text( "Beau travail, leth ennemith de l'autre monde ne vouth ferronth pluth jamaith de mlin."),
                Component.text( "Félicitation, thèth beau travail, je pense que vouth êteth assez résistant, je peux tidndre ma onatraite. Merci à vouth."),
                Component.text( "IMPOSSIBLE"),
        };

        Component[] names = new Component[]{
                Component.text( "Nain Roux I"),
                Component.text( "Nain Roux II"),
                Component.text( "Nain Roux III"),
                Component.text( "Nain Roux IV"),
                Component.text( "Nain Roux V"),
                Component.text( "Nain Roux MAX"),
        };

        VillagerLevel villager = new VillagerLevel(location,"Nain Roux I", Villager.Type.SWAMP, Villager.Profession.NITWIT, blessings, messages, tributes, names);
        villagersLevel.put("Nain Roux I", villager);
    }

    private void spawnMaddox(){
        Location location = new Location(world, 12072.5, 64, 1445.5, -90, 0);

        Tribute[] tributes = new Tribute[]{
                new Tribute(new ItemStack(Material.ARROW, 256)),
                new Tribute(new ItemStack(Material.TNT, 64)),
                new Tribute(new ItemStack(Material.FERMENTED_SPIDER_EYE, 300)),
                new Tribute(new ItemStack(Material.GHAST_TEAR, 64)),
                new Tribute(new ItemStack(Material.DRAGON_HEAD, 10)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new DamageEffect(0.4f)),
                new Blessing(new DamageEffect(0.6f)),
                new Blessing(new DamageEffect(0.8f)),
                new Blessing(new DamageEffect(1f)),
                new Blessing(new DamageEffect(1.5f)),
                new Blessing(new MessageEffect("IMPOSSIBLE")),
        };

        Component[] messages = new Component[]{
                Component.text( "Vous avez tué 3 poulets et vous voulez une récompense ? D'accord.. vous êtes un peu plus fort maintenant mais pas autant que moi quand même."),
                Component.text( "Vous voulez une récompense pour 4 creepers en moins sur la map ? N'importe quoi.. vous êtes un peu plus fort maintenant mais pas autant que moi quand même."),
                Component.text( "Comment on fait un oeil fermenté ? Avec un oeil... et tu le fermentes."),
                Component.text( "Pour les 6 ghasts en moins dans ce monde, je vous offre un peu plus de force."),
                Component.text( "C'est bon ? Vous avez fini de voyager ? Enfin... j'aurais été plus vite tout seul. Vous êtes maintenant assez fort. Mais toujours pas autant que moi."),
                Component.text( "IMPOSSIBLE"),
        };

        Component[] names = new Component[]{
                Component.text( "Maddox I"),
                Component.text( "Maddox II"),
                Component.text( "Maddox III"),
                Component.text( "Maddox VI"),
                Component.text( "Maddox V"),
                Component.text( "Maddox MAX"),
        };

        VillagerLevel villager = new VillagerLevel(location,"Maddox I", Villager.Type.TAIGA, Villager.Profession.BUTCHER, blessings, messages, tributes, names);
        villagersLevel.put("Maddox I", villager);
    }

    private void spawnThomas(){
        Location location = new Location(world, 12077.5, 63, 1464.5, 90, 0);

        Tribute[] tributes = new Tribute[]{
                new Tribute(new ItemStack(Material.DIRT, 1)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
                new Tribute(new ItemStack(Material.DIAMOND, 32)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
                new Tribute(new ItemStack(Material.WITHER_SKELETON_SKULL, 10)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
                new Tribute(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 3)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
                new Tribute(new ItemStack(Material.DRAGON_EGG, 1)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new MessageEffect("Niveau I réussi !")),
                new Blessing(new MessageEffect("Niveau II disponible pour Thomas Pesquet !")),
                new Blessing(new NetherEffect(), new MessageEffect("Niveau II réussi !")),
                new Blessing(new MessageEffect("Niveau III disponible pour Thomas Pesquet !")),
                new Blessing(new MessageEffect("Niveau III réussi !")),
                new Blessing(new MessageEffect("Niveau VI disponible pour Thomas Pesquet !")),
                new Blessing(new EndEffect(), new MessageEffect("Niveau VI réussi !")),
                new Blessing(new MessageEffect("Niveau V disponible pour Thomas Pesquet !")),
                new Blessing(new MessageEffect("Niveau V réussi !")),
                new Blessing(new MessageEffect("IMPOSSIBLE")),
        };

        Component[] messages = new Component[]{
                Component.text( "Merci, revenez dans 24h pour la suite."),
                Component.text( "Venez me voir."),
                Component.text( "Merci, revenez dans 48h pour la suite. Vous pouvez maintenant explorer le nether !"),
                Component.text( "Venez me voir."),
                Component.text( "Merci, revenez dans 72h pour la suite. Vous pouvez maintenant explorer un peu plus le Village et le Wilderness"),
                Component.text( "Venez me voir."),
                Component.text( "Merci, revenez dans 100h pour la suite. Vous pouvez maintenant explorer l'end ! "),
                Component.text( "Venez me voir."),
                Component.text( "Merci pour cet objet unique, pour te récompenser voici un peu de poisson. Vous pouvez maintenant explorer le Wilderness à l'infini."),
                Component.text( "IMPOSSIBLE"),
        };

        Component[] names = new Component[]{
                Component.text( "Thomas Pesquet I"),
                Component.text( "Thomas Pesquet I"),
                Component.text( "Thomas Pesquet II"),
                Component.text( "Thomas Pesquet II"),
                Component.text( "Thomas Pesquet III"),
                Component.text( "Thomas Pesquet III"),
                Component.text( "Thomas Pesquet IV"),
                Component.text( "Thomas Pesquet IV"),
                Component.text( "Thomas Pesquet V"),
                Component.text( "Thomas Pesquet V"),
        };

        VillagerLevel villager = new VillagerLevel(location,"Thomas Pesquet I", Villager.Type.SNOW, Villager.Profession.FISHERMAN, blessings, messages, tributes, names);
        villagersLevel.put("Thomas Pesquet I", villager);
    }

    private void spawnStuff(){
        Location location = new Location(world, 12073, 63, 1427, 0, 0);

        Tribute[] tributes = new Tribute[]{
                new Tribute(new ItemStack(Material.SADDLE, 1)),
                new Tribute(new ItemStack(Material.GOLDEN_APPLE, 64)),
                new Tribute(new ItemStack(Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, 6)),
                new Tribute(new ItemStack(Material.REDSTONE_BLOCK, 1400)),
                new Tribute(new ItemStack(Material.SPONGE, 48), new ItemStack(Material.WITHER_SKELETON_SKULL, 32)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new UnlockArmorEffect(LockedArmorType.LEATHER)),
                new Blessing(new UnlockArmorEffect(LockedArmorType.GOLD)),
                new Blessing(new UnlockArmorEffect(LockedArmorType.CHAINMAIL)),
                new Blessing(new UnlockArmorEffect(LockedArmorType.IRON)),
                new Blessing(new UnlockArmorEffect(LockedArmorType.DIAMOND)),
                new Blessing(new MessageEffect("IMPOSSIBLE")),
        };

        Component[] messages = new Component[]{
                Component.text( "Vous avez maintenant les capacités de construire vos propres armures en cuir d'une qualité incroyable."),
                Component.text("Vous pouvez maintenant construire la meilleure amure, la plus belle, l'armure en or ! La preuve de votre richesse et de votre force !"),
                Component.text( "J'ai créé une armure unique pour vous, essavez avec un peu de lave, vous verrez."),
                Component.text( "Vous pouvez maintenant construire une vraie armure grâce à moi."),
                Component.text( "La plus belle des armures est maintenant disponible, l'amure en diamant !"),
                Component.text( "IMPOSSIBLE"),
        };

        Component[] names = new Component[]{
                Component.text( "Cowboy"),
                Component.text( "Goldor"),
                Component.text( "Gardien"),
                Component.text( "IronMan"),
                Component.text( "Blue Moon"),
                Component.text( "Blue Moon"),
        };

        VillagerLevel villager = new VillagerLevel(location,"Comboy", Villager.Type.JUNGLE, Villager.Profession.ARMORER, blessings, messages, tributes, names);
        villagersLevel.put("Comboy", villager);
    }

    private void spawnTools(){
        Location location = new Location(world, 12184.5, 66, 1477.5, -90, 0);

        Tribute[] tributes = new Tribute[]{
                new Tribute(new ItemStack(Material.CRAFTING_TABLE, 64)),
                new Tribute(new ItemStack(Material.FURNACE, 64)),
                new Tribute(new ItemStack(Material.ANVIL, 20)),
                new Tribute(new ItemStack(Material.EMERALD_BLOCK, 1)),
                new Tribute(new ItemStack(Material.BROWN_CONCRETE, 576), new ItemStack(Material.STRIPPED_CHERRY_LOG, 576), new ItemStack(Material.COBBLED_DEEPSLATE_STAIRS, 576)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new UnlockToolEffect(LockedToolType.WOOD)),
                new Blessing(new UnlockToolEffect(LockedToolType.STONE)),
                new Blessing(new UnlockToolEffect(LockedToolType.IRON)),
                new Blessing(new UnlockToolEffect(LockedToolType.GOLD)),
                new Blessing(new UnlockToolEffect(LockedToolType.DIAMOND)),
                new Blessing(new MessageEffect("IMPOSSIBLE")),
        };

        Component[] messages = new Component[]{
                Component.text( "Les outils en bois sont très bien pour commencer."),
                Component.text( "Les outils en pierre sont les meilleurs sur le marché."),
                Component.text( "Les outils en fer sont super solide et très efficace."),
                Component.text( "Les meilleurs outils sont les outils en or."),
                Component.text( "I am a dwarf and I'm digging a hole. Diggy diggy hole, diggy diggy hole. Les outils en diamant sont disponibles !"),
                Component.text( "IMPOSSIBLE"),
        };

        Component[] names = new Component[]{
                Component.text( "Janod"),
                Component.text( "Pierre"),
                Component.text( "Léa"),
                Component.text( "Jeff Bezos"),
                Component.text( "Diggy Diggy Hole"),
                Component.text( "Diggy Diggy Hole"),
        };

        VillagerLevel villager = new VillagerLevel(location,"Janod", Villager.Type.TAIGA, Villager.Profession.WEAPONSMITH, blessings, messages, tributes, names);
        villagersLevel.put("Janod", villager);
    }

    private void spawnFrancois(){
        Location location = new Location(world, 12076.5, 64, 1483.5, 90, 0);

        Tribute[] tributes = new Tribute[]{
                new Tribute(new ItemStack(Material.BELL, 1)),
                new Tribute(new ItemStack(Material.CANDLE, 10)),
                new Tribute(new ItemStack(Material.YELLOW_CANDLE, 100)),
                new Tribute(new ItemStack(Material.RED_CANDLE, 200)),
                new Tribute(new ItemStack(Material.ORANGE_CANDLE, 300)),
                new Tribute(new ItemStack(Material.LIGHT_BLUE_CANDLE, 400)),
                new Tribute(new ItemStack(Material.GREEN_CANDLE, 400)),
                new Tribute(new ItemStack(Material.PINK_CANDLE, 500)),
                new Tribute(new ItemStack(Material.WHITE_CANDLE, 600)),
                new Tribute(new ItemStack(Material.TOTEM_OF_UNDYING, 27)),
                new Tribute(new ItemStack(Material.BEDROCK, 27)),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new DispelEffect(1)),
                new Blessing(new DispelEffect(2)),
                new Blessing(new DispelEffect(3)),
                new Blessing(new DispelEffect(4)),
                new Blessing(new DispelEffect(6)),
                new Blessing(new DispelEffect(7)),
                new Blessing(new DispelEffect(8)),
                new Blessing(new DispelEffect(9)),
                new Blessing(new DispelEffect(100)),
                new Blessing(new MessageEffect("IMPOSSIBLE")),
        };

        Component[] messages = new Component[]{
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
                Component.text( "IMPOSSIBLE"),
        };

        Component[] names = new Component[]{
                Component.text( "Léon XIII"),
                Component.text( "Pie X"),
                Component.text( "Benoît XV"),
                Component.text( "Pie XI"),
                Component.text( "Jean XXIII"),
                Component.text( "Paul VI"),
                Component.text( "Jean Paul I"),
                Component.text( "Jean Paul II"),
                Component.text( "Benoît XVI"),
                Component.text( "François I"),
                Component.text( "François I"),

        };

        VillagerLevel villager = new VillagerLevel(location,"Léon XIII", Villager.Type.JUNGLE, Villager.Profession.CLERIC, blessings, messages, tributes, names);
        villagersLevel.put("Léon XIII", villager);
    }

    private void spawnHeros(){
        Location location = new Location(world, 12091.5, 75, 1535.5, -180, 0);

        Tribute[] tributes = new Tribute[]{
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new DispelEffect(1)),
        };

        Component[] messages = new Component[]{
                Component.text( "IMPOSSIBLE"),
        };

        Component[] names = new Component[]{
                Component.text( "Héros I"),

        };

        VillagerLevel villager = new VillagerLevel(location,"Héros I", Villager.Type.SAVANNA, Villager.Profession.CARTOGRAPHER, blessings, messages, tributes, names);
        villagersLevel.put("Héros I", villager);
    }

    private void spawnPecheur1(){
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

        Component[] messages = new Component[]{
                Component.text( "Merci pour ce poisson, voici 1 émeraude."),
                Component.text( "Merci pour ce poisson, voici 1 émeraude."),
                Component.text( "Merci pour ce poisson, voici 1 émeraude."),
                Component.text( "Merci pour ce poisson, voici 1 émeraude."),
        };

        VillagerVendor villager = new VillagerVendor(location,"Bajau", Villager.Type.SWAMP, Villager.Profession.NITWIT, blessings, messages, items);
        villagersVendor.put("Bajau", villager);
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

        Component[] messages = new Component[]{
                Component.text( "Merci pour ces coquillages, voici 1 émeraude."),
                Component.text( "Merci pour ces names-tags, voici 2 émeraude."),
        };

        VillagerVendor villager = new VillagerVendor(location,"Sampan", Villager.Type.SWAMP, Villager.Profession.NITWIT, blessings, messages, items);
        villagersVendor.put("Sampan", villager);
    }

    private void spawnGolDRoger(){
        Location location = new Location(world, 12099.5, 69, 1474.5, 90, 0);

        ItemStack[] items = new ItemStack[]{
                new ItemStack(Material.BEDROCK, 64),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
        };

        Component[] messages = new Component[]{
                Component.text( "Merci pour ces bedrock, voici 1 émeraude."),
        };

        VillagerVendor villager = new VillagerVendor(location,"Gol D. Roger", Villager.Type.SWAMP, Villager.Profession.NITWIT, blessings, messages, items);
        villagersVendor.put("Gol D. Roger", villager);
    }

    private void spawnRiche(){
        Location location = new Location(world, 12072.5, 64, 1483.5, -90, 0);

        Tribute[] tributes = new Tribute[]{
                new Tribute(new ItemStack(Material.COPPER_BLOCK, 64), new ItemStack(Material.WAXED_COPPER_BLOCK, 64), new ItemStack(Material.EXPOSED_COPPER, 64), new ItemStack(Material.WAXED_EXPOSED_COPPER, 64), new ItemStack(Material.WEATHERED_COPPER, 64), new ItemStack(Material.WAXED_WEATHERED_COPPER, 64), new ItemStack(Material.OXIDIZED_COPPER, 64), new ItemStack(Material.WAXED_OXIDIZED_COPPER, 64), new ItemStack(Material.TORCHFLOWER, 64), new ItemStack(Material.PITCHER_PLANT, 64)),
                new Tribute(new ItemStack(Material.COAL, 128), new ItemStack(Material.CHARCOAL, 128), new ItemStack(Material.COPPER_INGOT, 128), new ItemStack(Material.IRON_INGOT, 128), new ItemStack(Material.GOLD_INGOT, 128), new ItemStack(Material.REDSTONE, 128), new ItemStack(Material.QUARTZ, 128), new ItemStack(Material.DIAMOND, 128), new ItemStack(Material.EMERALD, 64), new ItemStack(Material.FLINT, 128), new ItemStack(Material.GLOWSTONE_DUST, 128), new ItemStack(Material.NETHERITE_INGOT, 12)),
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

        Component[] messages = new Component[]{
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

        Component[] names = new Component[]{
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

        VillagerLevel villager = new VillagerLevel(location,"Ariscis I", Villager.Type.TAIGA, Villager.Profession.CLERIC, blessings, messages, tributes, names);
        villagersLevel.put("Ariscis I", villager);
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
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
        };

        Component[] messages = new Component[]{
                Component.text( "Merci pour ces algues, voici 1 émeraude."),
                Component.text( "Merci pour la viande, voici 1 émeraude."),
                Component.text( "Merci pour la viande, voici 1 émeraude."),
                Component.text( "Merci pour la viande, voici 1 émeraude."),
                Component.text( "Merci pour la viande, voici 1 émeraude."),
                Component.text( "Merci pour la viande, voici 1 émeraude."),
                Component.text( "Merci pour ces pattes, voici 1 émeraude."),
                Component.text( "Merci pour ce cuir, voici 1 émeraude."),
        };

        VillagerVendor villager = new VillagerVendor(location,"Fermier 1", Villager.Type.SWAMP, Villager.Profession.NITWIT, blessings, messages, items);
        villagersVendor.put("Fermier 1", villager);
    }

    private void spawnFermier2(){
        Location location = new Location(world, 12010.5, 64, 1466.5, -90, 0);

        ItemStack[] items = new ItemStack[]{
                new ItemStack(Material.HAY_BLOCK, 64),
                new ItemStack(Material.GOLDEN_CARROT, 64),
                new ItemStack(Material.BAKED_POTATO, 64),
                new ItemStack(Material.PUMPKIN_PIE, 64),
                new ItemStack(Material.SUGAR, 64),
                new ItemStack(Material.MELON, 64),
                new ItemStack(Material.GREEN_DYE, 64),
                new ItemStack(Material.COOKIE, 64),
                new ItemStack(Material.NETHER_WART_BLOCK, 64),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
                new Blessing(new ItemEffect(new ItemStack(Material.EMERALD, 1))),
        };

        Component[] messages = new Component[]{
                Component.text( "Merci pour ce blé, voici 1 émeraude."),
                Component.text( "Merci pour ces carottes, voici 1 émeraude."),
                Component.text( "Merci pour ces patates, voici 1 émeraude."),
                Component.text( "Merci pour ces tartes, voici 1 émeraude."),
                Component.text( "Merci pour ce sucre, voici 1 émeraude."),
                Component.text( "Merci pour ce melon, voici 1 émeraude."),
                Component.text( "Merci pour ce colorant, voici 1 émeraude."),
                Component.text( "Merci pour ces cookies, voici 1 émeraude."),
                Component.text( "Merci pour ces verrues, voici 1 émeraude."),
        };

        VillagerVendor villager = new VillagerVendor(location,"Fermier 2", Villager.Type.SWAMP, Villager.Profession.NITWIT, blessings, messages, items);
        villagersVendor.put("Fermier 2", villager);
    }

    public void DeleteVillagers() {
        for(VillagerEtat villager : villagersEtat.values()) {
            villager.getVillager().remove();
        }
        villagersEtat.clear();

        for(VillagerLevel villager : villagersLevel.values()) {
            villager.getVillager().remove();
        }
        villagersLevel.clear();

        for(VillagerVendor villager : villagersVendor.values()) {
            villager.getVillager().remove();
        }
        villagersVendor.clear();
    }
}
