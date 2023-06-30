package fr.miuby.survi18.village;

import com.sun.tools.javac.jvm.Items;
import fr.miuby.survi18.blessing.*;
import fr.miuby.survi18.Tribute;
import fr.miuby.survi18.locked_item.LockedArmorType;
import fr.miuby.survi18.locked_item.LockedToolType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class Village {
    private final World world;
    private final Map<String, VillagerLevel> villagersLevel = new HashMap<>();
    private final Map<String, VillagerEtat> villagersEtat = new HashMap<>();

    public Village(World world) {
        this.world = world;

        spawnEdward();
        spawnNain();
        spawnMaddox();
        spawnThomas();
        spawnStuff();
        spawnTools();

    }

    public World getWorld() { return world; }

    public Map<String, VillagerLevel> getVillagersLevel() {
        return villagersLevel;
    }

    public Map<String, VillagerEtat> getVillagersEtat() {
        return villagersEtat;
    }

    private void spawnEdward(){
        Location location = new Location(world, 12073, 64, 1509, 180, 0);

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
                new Tribute(new ItemStack(Material.NETHER_STAR, 10)),
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
                new Blessing(new MaxHealthEffect(36)),
                new Blessing(new MaxHealthEffect(40)),
                new Blessing(new RegenEffect(), new FlyEffect()),
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
                Component.text( "Félicitation, vous voici maintenant à 36 HP. J'ai presque fini la potion magique."),
                Component.text( "Bravo, vous avez maintenant 40 HP. Il me manque que quelques étoiles du nether et je suis prêt. Allez me le chercher, vite vite vite !!!!"),
                Component.text( "Félicitations, j'ai fini !! Vous pouvez voler, vous pouvez regagner votre vie sans pomme !! AHAHAH !! Je suis le meilleur !!"),
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

        VillagerLevel villager = new VillagerLevel(location,"Edward Jenner I", Villager.Type.SAVANNA, Villager.Profession.FARMER, tributes, blessings, messages, names);
        villagersLevel.put("Edward Jenner I", villager);
    }

    private void spawnNain(){
        Location location = new Location(world, 12139, 64, 1463, 90, 0);

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

        VillagerLevel villager = new VillagerLevel(location,"Nain Roux I", Villager.Type.SWAMP, Villager.Profession.NITWIT, tributes, blessings, messages, names);
        villagersLevel.put("Nain Roux I", villager);
    }

    private void spawnMaddox(){
        Location location = new Location(world, 12016, 64, 1463, -90, 0);

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

        VillagerLevel villager = new VillagerLevel(location,"Maddox I", Villager.Type.TAIGA, Villager.Profession.BUTCHER, tributes, blessings, messages, names);
        villagersLevel.put("Maddox I", villager);
    }

    private void spawnThomas(){
        Location location = new Location(world, 12071, 64, 1509, 180, 0);

        Tribute[] tributes = new Tribute[]{
                new Tribute(new ItemStack(Material.ARROW, 256)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
                new Tribute(new ItemStack(Material.TNT, 64)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
                new Tribute(new ItemStack(Material.FERMENTED_SPIDER_EYE, 300)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
                new Tribute(new ItemStack(Material.GHAST_TEAR, 64)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
                new Tribute(new ItemStack(Material.DRAGON_HEAD, 10)),
                new Tribute(new ItemStack(Material.BEDROCK, 1)),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new MessageEffect("Niveau I réussi !")),
                new Blessing(new MessageEffect("Niveau II disponible pour Thomas Pesquet !")),
                new Blessing(new MessageEffect("Niveau II réussi !")),
                new Blessing(new MessageEffect("Niveau III disponible pour Thomas Pesquet !")),
                new Blessing(new MessageEffect("Niveau III réussi !")),
                new Blessing(new MessageEffect("Niveau VI disponible pour Thomas Pesquet !")),
                new Blessing(new MessageEffect("Niveau VI réussi !")),
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
                Component.text( "Merci pour cet objet unique, pour te récompenser voici un peu de poisson. Vous pouvez maintenant explorer le Wilderness."),
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

        VillagerLevel villager = new VillagerLevel(location,"Thomas Pesquet I", Villager.Type.SNOW, Villager.Profession.FISHERMAN, tributes, blessings, messages, names);
        villagersLevel.put("Thomas Pesquet I", villager);
    }

    private void spawnStuff(){
        Location location = new Location(world, 12073, 63, 1427, 0, 0);

        Tribute[] tributes = new Tribute[]{
                new Tribute(new ItemStack(Material.SADDLE, 1)),
                new Tribute(new ItemStack(Material.GOLDEN_APPLE, 64)),
                new Tribute(new ItemStack(Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, 6)),
                new Tribute(new ItemStack(Material.REDSTONE_BLOCK, 600)),
                new Tribute(new ItemStack(Material.SPONGE, 10)),
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
                Component.text( "IMPOSSIBLE"),
        };

        VillagerLevel villager = new VillagerLevel(location,"Comboy", Villager.Type.JUNGLE, Villager.Profession.ARMORER, tributes, blessings, messages, names);
        villagersLevel.put("Comboy", villager);
    }

    private void spawnTools(){
        Location location = new Location(world, 12079, 63, 1427, 0, 0);

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
                Component.text( "IMPOSSIBLE"),
        };

        VillagerLevel villager = new VillagerLevel(location,"Janod", Villager.Type.TAIGA, Villager.Profession.WEAPONSMITH, tributes, blessings, messages, names);
        villagersLevel.put("Janod", villager);
    }

/*
    private void spawnForgeron(){
        VillagerEtat villagerEtat = new VillagerEtat(new Location(world, 12, 70, -46, 290, 0),
                "Forgeron", Villager.Type.PLAINS, Villager.Profession.WEAPONSMITH);
        villagers.put("Forgeron", villagerEtat);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        items.put(Material.COPPER_INGOT, new ItemEtat(Material.COPPER_INGOT, 75, false));
        items.put(Material.COAL, new ItemEtat(Material.COAL, 100, false));
        items.put(Material.IRON_INGOT, new ItemEtat(Material.IRON_INGOT, 500, false));
        items.put(Material.GOLD_INGOT, new ItemEtat(Material.GOLD_INGOT, 500, false));
        items.put(Material.EMERALD, new ItemEtat(Material.EMERALD, 500, false));
        items.put(Material.DIAMOND, new ItemEtat(Material.DIAMOND, 55000, false));
        items.put(Material.NETHERITE_SCRAP, new ItemEtat(Material.NETHERITE_SCRAP, 1000000, false));

        villagerEtat.SetItems(items);
    }

    private void spawnArmurier() {
        VillagerEtat villagerEtat = new VillagerEtat(new Location(world, 11.5, 70, -36.2, 250, 0),
                "Armurier", Villager.Type.PLAINS, Villager.Profession.ARMORER);
        villagers.put("Armurier", villagerEtat);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        items.put(Material.IRON_HELMET, new ItemEtat(Material.IRON_HELMET, 5000, true));
        items.put(Material.IRON_CHESTPLATE, new ItemEtat(Material.IRON_CHESTPLATE, 20000, true));
        items.put(Material.IRON_LEGGINGS, new ItemEtat(Material.IRON_LEGGINGS, 10000, true));
        items.put(Material.IRON_BOOTS, new ItemEtat(Material.IRON_BOOTS, 5000, true));
        items.put(Material.GOLDEN_HELMET, new ItemEtat(Material.GOLDEN_HELMET, 5000, true));
        items.put(Material.GOLDEN_CHESTPLATE, new ItemEtat(Material.GOLDEN_CHESTPLATE, 10000, true));
        items.put(Material.GOLDEN_LEGGINGS, new ItemEtat(Material.GOLDEN_LEGGINGS, 8000, true));
        items.put(Material.GOLDEN_BOOTS, new ItemEtat(Material.GOLDEN_BOOTS, 5000, true));
        items.put(Material.DIAMOND_HELMET, new ItemEtat(Material.DIAMOND_HELMET, 500000, true));
        items.put(Material.DIAMOND_CHESTPLATE, new ItemEtat(Material.DIAMOND_CHESTPLATE, 1000000, true));
        items.put(Material.DIAMOND_LEGGINGS, new ItemEtat(Material.DIAMOND_LEGGINGS, 500000, true));
        items.put(Material.DIAMOND_BOOTS, new ItemEtat(Material.DIAMOND_BOOTS, 500000, true));
        items.put(Material.NETHERITE_INGOT, new ItemEtat(Material.NETHERITE_INGOT, 10000000, true));

        villagerEtat.SetItems(items);
    }

    private void spawnPretre() {
        VillagerEtat villagerEtat = new VillagerEtat(new Location(world, -13.5, 75, -1.5, 90, 0),
                "Pretre", Villager.Type.PLAINS, Villager.Profession.CLERIC);
        villagers.put("Pretre", villagerEtat);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        items.put(Material.TOTEM_OF_UNDYING, new ItemEtat(Material.TOTEM_OF_UNDYING, 1000000, true));
        items.put(Material.CANDLE, new ItemEtat(Material.CANDLE, 1000, true));
        items.put(Material.BEACON, new ItemEtat(Material.BEACON, 10000000, true, new ItemStack(Material.NETHER_STAR)));
        items.put(Material.FIREWORK_ROCKET, new ItemEtat(Material.FIREWORK_ROCKET, 40000, true, new ItemStack(Material.PAPER)));

        villagerEtat.SetItems(items);

    }

    private void spawnMarchant() {
        VillagerEtat villagerEtat = new VillagerEtat(new Location(world, 27, 70, 34.5, 180, 0),
                "Marchant", Villager.Type.DESERT, Villager.Profession.NONE);
        villagers.put("Marchant", villagerEtat);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        items.put(Material.BAKED_POTATO, new ItemEtat(Material.BAKED_POTATO, 50, false));
        items.put(Material.CARROT, new ItemEtat(Material.CARROT, 3, false));
        items.put(Material.BREAD, new ItemEtat(Material.BREAD, 10, false));
        items.put(Material.SUGAR, new ItemEtat(Material.SUGAR, 3, false));
        items.put(Material.BEETROOT_SOUP, new ItemEtat(Material.BEETROOT_SOUP, 100, false));

        villagerEtat.SetItems(items);
    }

    private void spawnMarchand() {
        VillagerEtat villagerEtat = new VillagerEtat(new Location(world, 38, 70, 36.5, 180, 0),
                "Marchand", Villager.Type.DESERT, Villager.Profession.NONE);
        villagers.put("Marchand", villagerEtat);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        items.put(Material.BAKED_POTATO, new ItemEtat(Material.BAKED_POTATO, 50, false));
        items.put(Material.CARROT, new ItemEtat(Material.CARROT, 3, false));
        items.put(Material.BREAD, new ItemEtat(Material.BREAD, 10, false));
        items.put(Material.SUGAR, new ItemEtat(Material.SUGAR, 3, false));
        items.put(Material.BEETROOT_SOUP, new ItemEtat(Material.BEETROOT_SOUP, 100, false));

        villagerEtat.SetItems(items);
    }

    private void spawnMarchande() {
        VillagerEtat villagerEtat = new VillagerEtat(new Location(world, 40.5, 70, 25, 90, 0),
                "Marchande", Villager.Type.DESERT, Villager.Profession.NITWIT);
        villagers.put("Marchande", villagerEtat);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        items.put(Material.BAKED_POTATO, new ItemEtat(Material.BAKED_POTATO, 50, false));
        items.put(Material.CARROT, new ItemEtat(Material.CARROT, 3, false));
        items.put(Material.BREAD, new ItemEtat(Material.BREAD, 10, false));
        items.put(Material.SUGAR, new ItemEtat(Material.SUGAR, 3, false));
        items.put(Material.BEETROOT_SOUP, new ItemEtat(Material.BEETROOT_SOUP, 100, false));

        villagerEtat.SetItems(items);
    }

    private void spawnAubergiste() {
        VillagerEtat villagerEtat = new VillagerEtat(new Location(world, 10, 67, 45.5, 0, 0),
                "Aubergiste", Villager.Type.SWAMP, Villager.Profession.BUTCHER);
        villagers.put("Aubergiste", villagerEtat);

        ItemStack shield = new ItemStack(Material.SHIELD);
        ItemMeta shieldMeta = shield.getItemMeta();
        shieldMeta.displayName(Component.text("Nuit régénératrice"));
        shield.setItemMeta(shieldMeta);

        ItemStack sugar = new ItemStack(Material.SUGAR);
        ItemMeta sugarMeta = sugar.getItemMeta();
        sugarMeta.displayName(Component.text("Nuit express"));
        sugar.setItemMeta(sugarMeta);

        ItemStack rabbit = new ItemStack(Material.RABBIT_FOOT);
        ItemMeta rabbitMeta = rabbit.getItemMeta();
        rabbitMeta.displayName(Component.text("Nuit légère"));
        rabbit.setItemMeta(rabbitMeta);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        items.put(Material.SHIELD, new ItemEtat(shield, 250000, true));
        items.put(Material.SUGAR, new ItemEtat(sugar, 250000, true));
        items.put(Material.RABBIT_FOOT, new ItemEtat(rabbit, 250000, true));

        villagerEtat.SetItems(items);
    }

    private void spawnTavernier() {
        VillagerEtat villagerEtat = new VillagerEtat(new Location(world, 68, 71.5, 26.5, 90, 0),
                "Tavernier", Villager.Type.PLAINS, Villager.Profession.LEATHERWORKER);
        villagers.put("Tavernier", villagerEtat);

        ItemStack item = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setColor(Color.ORANGE);
        meta.displayName(Component.text("Whisky coca DELUXE"));
        meta.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 300, 3), false);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.CONFUSION, 2400, 1), false);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 4800, 2), false);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.HEAL, 1, 2), false);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 4800, 2), false);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.JUMP, 2400, 0), false);
        item.setItemMeta(meta);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        items.put(Material.POTION, new ItemEtat(item, 500000, true));

        villagerEtat.SetItems(items);
    }

    private void spawnMaire() {
        VillagerEtat villagerEtat = new VillagerEtat(new Location(world, -10.5, 85.8, -43.5, 90, 0),
                "Maire", Villager.Type.PLAINS, Villager.Profession.CARTOGRAPHER);
        villagers.put("Maire", villagerEtat);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        ItemStack itemStack = new ItemStack(Material.FEATHER);
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(Component.text("Vol"));
        itemStack.setItemMeta(meta);
        //items.put(Material.FEATHER, new ItemEtat(itemStack, 1, true));
        items.put(Material.APPLE, new ItemEtat(Material.APPLE, 10000, true));

        villagerEtat.SetItems(items);
    }

    private void spawnProfesseur() {
        VillagerEtat villagerEtat = new VillagerEtat(new Location(world, -105.5, 77, 40.5, 315, 0),
                "Professeur", Villager.Type.PLAINS, Villager.Profession.LIBRARIAN);
        villagers.put("Professeur", villagerEtat);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        items.put(Material.APPLE, new ItemEtat(Material.APPLE, 10000, true));

        villagerEtat.SetItems(items);
    }

    private void spawnBibliothecaire() {
        VillagerEtat villagerEtat = new VillagerEtat(new Location(world, -101.5, 82, 87.5, 180, 0),
                "Bibliothécaire", Villager.Type.SWAMP, Villager.Profession.LIBRARIAN);
        villagers.put("Bibliothécaire", villagerEtat);

        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
        meta.addStoredEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 5,true);
        item.setItemMeta(meta);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        items.put(Material.ENCHANTED_BOOK, new ItemEtat(item, 15000000, true));

        villagerEtat.SetItems(items);
    }

    private void spawnBanquier() {
        VillagerEtat villagerEtat = new VillagerEtat(new Location(world, -131.5, 68.8, 54.25, 180, 0),
                "Banquier", Villager.Type.PLAINS, Villager.Profession.NONE);
        villagers.put("Banquier", villagerEtat);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        items.put(Material.APPLE, new ItemEtat(Material.APPLE, 10000, true));

        villagerEtat.SetItems(items);
    }

    private void spawnNetherGardian() {
        VillagerEtat villagerEtat = new VillagerEtat(new Location(world,19, 70, -46, 290, 0),
                "Gardien du nether", Villager.Type.PLAINS, Villager.Profession.NONE);
        villagers.put("Gardien du nether", villagerEtat);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        items.put(Material.APPLE, new ItemEtat(Material.APPLE, 1, true));

        villagerEtat.SetItems(items);
    }

    private void spawnEndGardian() {
        VillagerEtat villagerEtat = new VillagerEtat(new Location(world,20, 70, -46, 290, 0),
                "Gardien de l'end", Villager.Type.PLAINS, Villager.Profession.NONE);
        villagers.put("Gardien de l'end", villagerEtat);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        items.put(Material.APPLE, new ItemEtat(Material.APPLE, 1, true));

        villagerEtat.SetItems(items);
    }*/

    public void DeleteVillagers() {
        for(VillagerEtat villager : villagersEtat.values()) {
            villager.getVillager().remove();
        }
        villagersEtat.clear();

        for(VillagerLevel villager : villagersLevel.values()) {
            villager.getVillager().remove();
        }
        villagersLevel.clear();
    }
}
