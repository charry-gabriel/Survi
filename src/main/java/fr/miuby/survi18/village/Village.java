package fr.miuby.survi18.village;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class Village {
    private World world;
    private Map<String, VillagerEtat> villagers = new HashMap<String, VillagerEtat>();

    public Village(World world) {
        this.world = world;

        spawnForgeron();
        spawnArmurier();
        spawnPretre();
        spawnMarchand();
        spawnMarchant();
        spawnMarchande();

        spawnAubergiste();
        spawnTavernier();
        spawnMaire();
        spawnProfesseur();
        spawnBibliothecaire();
        spawnBanquier();
    }

    public World getWorld() { return world; }

    public Map<String, VillagerEtat> getVillagers() {
        return villagers;
    }

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
        ItemMeta shieldMeta = (ItemMeta) shield.getItemMeta();
        shieldMeta.displayName(Component.text("Nuit régénératrice"));
        shield.setItemMeta(shieldMeta);

        ItemStack sugar = new ItemStack(Material.SUGAR);
        ItemMeta sugarMeta = (ItemMeta) sugar.getItemMeta();
        sugarMeta.displayName(Component.text("Nuit express"));
        sugar.setItemMeta(sugarMeta);

        ItemStack rabbit = new ItemStack(Material.RABBIT_FOOT);
        ItemMeta rabbitMeta = (ItemMeta) rabbit.getItemMeta();
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
    }

    public void DeleteVillagers() {
        for(VillagerEtat villager : villagers.values()) {
            villager.getVillager().remove();
        }
        villagers.clear();
    }
}
