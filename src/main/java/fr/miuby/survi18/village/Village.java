package fr.miuby.survi18.village;

import fr.miuby.survi18.blessing.Blessing;
import fr.miuby.survi18.Tribute;
import fr.miuby.survi18.blessing.DamageEffect;
import fr.miuby.survi18.blessing.MaxHealthEffect;
import fr.miuby.survi18.blessing.ResistanceEffect;
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
        Location location = new Location(world, 12, 70, -46, 290, 0);

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
        };

        VillagerLevel villager = new VillagerLevel(location,"Edward Jenner", Villager.Type.PLAINS, Villager.Profession.WEAPONSMITH, tributes, blessings);
        villagersLevel.put("Edward Jenner", villager);
    }

    private void spawnNain(){
        Location location = new Location(world, 12, 70, -46, 290, 0);

        Tribute[] tributes = new Tribute[]{
                new Tribute(new ItemStack(Material.DIORITE_WALL, 64)),
                new Tribute(new ItemStack(Material.AMETHYST_BLOCK, 128)),
                new Tribute(new ItemStack(Material.RED_NETHER_BRICKS, 192)),
                new Tribute(new ItemStack(Material.SCULK_CATALYST, 256)),
                new Tribute(new ItemStack(Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, 320)),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new ResistanceEffect(0.4f)),
                new Blessing(new ResistanceEffect(0.6f)),
                new Blessing(new ResistanceEffect(1f)),
                new Blessing(new ResistanceEffect(1.2f)),
                new Blessing(new ResistanceEffect(1.4f)),
        };

        VillagerLevel villager = new VillagerLevel(location,"Nain", Villager.Type.PLAINS, Villager.Profession.WEAPONSMITH, tributes, blessings);
        villagersLevel.put("Nain", villager);
    }

    private void spawnMaddox(){
        Location location = new Location(world, 12, 70, -46, 290, 0);

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

        VillagerLevel villager = new VillagerLevel(location,"Maddox", Villager.Type.PLAINS, Villager.Profession.WEAPONSMITH, tributes, blessings);
        villagersLevel.put("Maddox", villager);
    }


    private void spawnStuff(){
        Location location = new Location(world, 12, 70, -46, 290, 0);

        Tribute[] tributes = new Tribute[]{
                new Tribute(new ItemStack(Material.SADDLE, 1)),
                new Tribute(new ItemStack(Material.GOLDEN_APPLE, 64)),
                new Tribute(new ItemStack(Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, 6)),
                new Tribute(new ItemStack(Material.REDSTONE_BLOCK, 600)),
                new Tribute(new ItemStack(Material.SPONGE, 10)),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new DamageEffect(2)),
                new Blessing(new DamageEffect(2)),
                new Blessing(new DamageEffect(2)),
                new Blessing(new DamageEffect(2)),
                new Blessing(new DamageEffect(2)),
        };

        VillagerLevel villager = new VillagerLevel(location,"Stuff", Villager.Type.PLAINS, Villager.Profession.WEAPONSMITH, tributes, blessings);
        villagersLevel.put("Stuff", villager);
    }

    private void spawnTools(){
        Location location = new Location(world, 12, 70, -46, 290, 0);

        Tribute[] tributes = new Tribute[]{
                new Tribute(new ItemStack(Material.CRAFTING_TABLE, 64)),
                new Tribute(new ItemStack(Material.FURNACE, 64)),
                new Tribute(new ItemStack(Material.ANVIL, 20)),
                new Tribute(new ItemStack(Material.EMERALD_BLOCK, 1)),
                new Tribute(new ItemStack(Material.BROWN_CONCRETE, 576), new ItemStack(Material.STRIPPED_CHERRY_LOG, 576), new ItemStack(Material.COBBLED_DEEPSLATE_STAIRS, 576)),
        };

        Blessing[] blessings = new Blessing[]{
                new Blessing(new DamageEffect(2)),
                new Blessing(new DamageEffect(2)),
                new Blessing(new DamageEffect(2)),
                new Blessing(new DamageEffect(2)),
                new Blessing(new DamageEffect(2)),
        };

        VillagerLevel villager = new VillagerLevel(location,"Outils", Villager.Type.PLAINS, Villager.Profession.WEAPONSMITH, tributes, blessings);
        villagersLevel.put("Outils", villager);
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
