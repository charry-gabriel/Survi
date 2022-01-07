package fr.miuby.survi18.village;

import fr.miuby.survi18.Position;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

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
        spawnNetherGardian();
        spawnEndGardian();
    }

    public World getWorld() { return world; }

    public Map<String, VillagerEtat> getVillagers() {
        return villagers;
    }

    private void spawnForgeron(){
        Villager villager = spawnVillager(new Position(12, 70, -46, 290), "Forgeron", Villager.Type.PLAINS, Villager.Profession.WEAPONSMITH);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        items.put(Material.COPPER_INGOT, new ItemEtat(Material.COPPER_INGOT, 75, false));
        items.put(Material.COAL, new ItemEtat(Material.COAL, 100, false));
        items.put(Material.IRON_INGOT, new ItemEtat(Material.IRON_INGOT, 500, false));
        items.put(Material.GOLD_INGOT, new ItemEtat(Material.GOLD_INGOT, 500, false));
        items.put(Material.EMERALD, new ItemEtat(Material.EMERALD, 500, false));
        items.put(Material.DIAMOND, new ItemEtat(Material.DIAMOND, 55000, false));
        items.put(Material.NETHERITE_SCRAP, new ItemEtat(Material.NETHERITE_SCRAP, 1000000, false));

        VillagerEtat villagerEtat = new VillagerEtat(villager, items);

        villagers.put("Forgeron", villagerEtat);
    }

    private void spawnArmurier() {
        Villager villager = spawnVillager(new Position(11.5, 70, -36.2, 250), "Armurier", Villager.Type.PLAINS, Villager.Profession.ARMORER);

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

        VillagerEtat villagerEtat = new VillagerEtat(villager, items);

        villagers.put("Armurier", villagerEtat);
    }

    private void spawnPretre() {
        Villager villager = spawnVillager(new Position(-19.5, 73, -1.5, 180), "Pretre", Villager.Type.PLAINS, Villager.Profession.CLERIC);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        items.put(Material.TOTEM_OF_UNDYING, new ItemEtat(Material.TOTEM_OF_UNDYING, 10000000, true));
        items.put(Material.CANDLE, new ItemEtat(Material.CANDLE, 1000, true));
        items.put(Material.BEACON, new ItemEtat(Material.BEACON, 10000000, true, new ItemStack(Material.NETHER_STAR)));

        VillagerEtat villagerEtat = new VillagerEtat(villager, items);

        villagers.put("Pretre", villagerEtat);
    }

    private void spawnMarchant() {
        Villager villager = spawnVillager(new Position(27, 70, 34.5, 180), "Marchant", Villager.Type.DESERT, Villager.Profession.NONE);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        items.put(Material.BAKED_POTATO, new ItemEtat(Material.BAKED_POTATO, 50, false));
        items.put(Material.CARROT, new ItemEtat(Material.CARROT, 3, false));
        items.put(Material.BREAD, new ItemEtat(Material.BREAD, 10, false));
        items.put(Material.SUGAR, new ItemEtat(Material.SUGAR, 3, false));
        items.put(Material.BEETROOT_SOUP, new ItemEtat(Material.BEETROOT_SOUP, 100, false));

        VillagerEtat villagerEtat = new VillagerEtat(villager, items);

        villagers.put("Marchant", villagerEtat);
    }

    private void spawnMarchand() {
        Villager villager = spawnVillager(new Position(38, 70, 36.5, 180), "Marchand", Villager.Type.DESERT, Villager.Profession.NONE);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        items.put(Material.BAKED_POTATO, new ItemEtat(Material.BAKED_POTATO, 50, false));
        items.put(Material.CARROT, new ItemEtat(Material.CARROT, 3, false));
        items.put(Material.BREAD, new ItemEtat(Material.BREAD, 10, false));
        items.put(Material.SUGAR, new ItemEtat(Material.SUGAR, 3, false));
        items.put(Material.BEETROOT_SOUP, new ItemEtat(Material.BEETROOT_SOUP, 100, false));

        VillagerEtat villagerEtat = new VillagerEtat(villager, items);

        villagers.put("Marchand", villagerEtat);
    }

    private void spawnMarchande() {
        Villager villager = spawnVillager(new Position(40.5, 70, 25, 90), "Marchande", Villager.Type.DESERT, Villager.Profession.NITWIT);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        items.put(Material.BAKED_POTATO, new ItemEtat(Material.BAKED_POTATO, 50, false));
        items.put(Material.CARROT, new ItemEtat(Material.CARROT, 3, false));
        items.put(Material.BREAD, new ItemEtat(Material.BREAD, 10, false));
        items.put(Material.SUGAR, new ItemEtat(Material.SUGAR, 3, false));
        items.put(Material.BEETROOT_SOUP, new ItemEtat(Material.BEETROOT_SOUP, 100, false));

        VillagerEtat villagerEtat = new VillagerEtat(villager, items);

        villagers.put("Marchande", villagerEtat);
    }

    private void spawnAubergiste() {
        Villager villager = spawnVillager(new Position(10, 67, 45.5, 0), "Aubergiste", Villager.Type.SWAMP, Villager.Profession.BUTCHER);

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
        items.put(Material.SHIELD, new ItemEtat(shield, 1000000, true));
        items.put(Material.SUGAR, new ItemEtat(sugar, 1000000, true));
        items.put(Material.RABBIT_FOOT, new ItemEtat(rabbit, 1000000, true));

        VillagerEtat villagerEtat = new VillagerEtat(villager, items);

        villagers.put("Aubergiste", villagerEtat);
    }

    private void spawnTavernier() {
        Villager villager = spawnVillager(new Position(68, 71.5, 26.5, 90), "Tavernier", Villager.Type.PLAINS, Villager.Profession.LEATHERWORKER);

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
        items.put(Material.POTION, new ItemEtat(item, 1000000, true));

        VillagerEtat villagerEtat = new VillagerEtat(villager, items);

        villagers.put("Tavernier", villagerEtat);
    }

    private void spawnMaire() {
        Villager villager = spawnVillager(new Position(15, 70, -46, 290), "Maire", Villager.Type.PLAINS, Villager.Profession.NONE);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        ItemStack itemStack = new ItemStack(Material.FEATHER);
        ItemMeta meta = itemStack.getItemMeta();
        meta.displayName(Component.text("Vol"));
        itemStack.setItemMeta(meta);
        items.put(Material.FEATHER, new ItemEtat(itemStack, 1, true));

        VillagerEtat villagerEtat = new VillagerEtat(villager, items);

        villagers.put("Maire", villagerEtat);
    }

    private void spawnProfesseur() {
        Villager villager = spawnVillager(new Position(16, 70, -46, 290), "Professeur", Villager.Type.PLAINS, Villager.Profession.NONE);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        items.put(Material.APPLE, new ItemEtat(Material.APPLE, 1, true));

        VillagerEtat villagerEtat = new VillagerEtat(villager, items);

        villagers.put("Professeur", villagerEtat);
    }

    private void spawnBibliothecaire() {
        Villager villager = spawnVillager(new Position(17, 70, -46, 290), "Bibliothécaire", Villager.Type.PLAINS, Villager.Profession.NONE);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        items.put(Material.APPLE, new ItemEtat(Material.APPLE, 1, true));

        VillagerEtat villagerEtat = new VillagerEtat(villager, items);

        villagers.put("Bibliothécaire", villagerEtat);
    }

    private void spawnBanquier() {
        Villager villager = spawnVillager(new Position(18, 70, -46, 290), "Banquier", Villager.Type.PLAINS, Villager.Profession.NONE);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        items.put(Material.APPLE, new ItemEtat(Material.APPLE, 1, true));

        VillagerEtat villagerEtat = new VillagerEtat(villager, items);

        villagers.put("Banquier", villagerEtat);
    }

    private void spawnNetherGardian() {
        Villager villager = spawnVillager(new Position(19, 70, -46, 290), "Gardien du nether", Villager.Type.PLAINS, Villager.Profession.NONE);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        items.put(Material.APPLE, new ItemEtat(Material.APPLE, 1, true));

        VillagerEtat villagerEtat = new VillagerEtat(villager, items);

        villagers.put("Gardien du nether", villagerEtat);
    }

    private void spawnEndGardian() {
        Villager villager = spawnVillager(new Position(20, 70, -46, 290), "Gardien de l'end", Villager.Type.PLAINS, Villager.Profession.NONE);

        LinkedHashMap<Material, ItemEtat> items = new LinkedHashMap<>();
        items.put(Material.APPLE, new ItemEtat(Material.APPLE, 1, true));

        VillagerEtat villagerEtat = new VillagerEtat(villager, items);

        villagers.put("Gardien de l'end", villagerEtat);
    }

    private Villager spawnVillager(Position position, String name, Villager.Type type, Villager.Profession profession) {
        Location loc = new Location(world, position.x, position.y, position.z);
        loc.setYaw(position.yaw);

        Villager v = (Villager) world.spawnEntity(loc, EntityType.VILLAGER);
        v.setCustomName(name);
        v.setVillagerType(type);
        v.setProfession(profession);
        v.setAI(false);
        v.setCollidable(false);

        return  v;
    }

    public void DeleteVillagers() {
        for(VillagerEtat villager : villagers.values()) {
            villager.getVillager().remove();
        }
        villagers.clear();
    }
}
