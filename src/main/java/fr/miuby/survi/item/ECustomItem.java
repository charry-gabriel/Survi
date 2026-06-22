package fr.miuby.survi.item;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.growth_item.GrowthItems;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER;
import static org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR;

@Getter
public enum ECustomItem {
    GROWTH_MINER_HELMET(Material.LEATHER_HELMET, item -> {
        ItemMeta preMeta = item.getItemMeta();
        createGrowthItem(preMeta, "GROWTH_MINER_HELMET");
        item.setItemMeta(preMeta);
        new CustomItemBuilder(item, "Casque du Mineur")
            .name("Casque du Mineur 1", NamedTextColor.GRAY)
            .leatherArmor(TrimMaterial.GOLD, TrimPattern.TIDE, Color.fromRGB(11184810))
            .addAttribute(Attribute.MINING_EFFICIENCY, 0, ADD_NUMBER, EquipmentSlotGroup.HEAD) //évolutif
            .addAttribute(Attribute.ARMOR, -10, ADD_NUMBER, EquipmentSlotGroup.HEAD)
            .addAttribute(Attribute.MAX_HEALTH, -10, ADD_NUMBER, EquipmentSlotGroup.HEAD)
            .addAttribute(Attribute.BLOCK_INTERACTION_RANGE, 2, ADD_NUMBER, EquipmentSlotGroup.HEAD)
            .addAttribute(Attribute.MOVEMENT_SPEED, -0.03, ADD_NUMBER, EquipmentSlotGroup.HEAD)
            .addAttribute(Attribute.SCALE, -0.34, ADD_NUMBER, EquipmentSlotGroup.HEAD)
            .unbreakable()
            .addItemFlag(ItemFlag.HIDE_DYE)
            .addItemFlag(ItemFlag.HIDE_ARMOR_TRIM);
    }),

    GROWTH_LUMBERJACK_CHESPLATE(Material.LEATHER_CHESTPLATE, item -> {
        ItemMeta preMeta = item.getItemMeta();
        createGrowthItem(preMeta, "GROWTH_LUMBERJACK_CHESPLATE");
        item.setItemMeta(preMeta);
        new CustomItemBuilder(item, "Plastron du Bûcheron")
            .name("Plastron du Bûcheron 1", NamedTextColor.DARK_GREEN)
            .leatherArmor(TrimMaterial.EMERALD, TrimPattern.DUNE, Color.fromRGB(43520))
            .addAttribute(Attribute.MINING_EFFICIENCY, 0, ADD_NUMBER, EquipmentSlotGroup.HEAD) //évolutif
            .addAttribute(Attribute.ARMOR, -20, ADD_NUMBER, EquipmentSlotGroup.HEAD)
            .addAttribute(Attribute.MAX_HEALTH, 10, ADD_NUMBER, EquipmentSlotGroup.HEAD)
            .addAttribute(Attribute.BLOCK_INTERACTION_RANGE, 2, ADD_NUMBER, EquipmentSlotGroup.HEAD)
            .addAttribute(Attribute.MOVEMENT_SPEED, -0.03, ADD_NUMBER, EquipmentSlotGroup.HEAD)
            .addAttribute(Attribute.SCALE, 0.3, ADD_NUMBER, EquipmentSlotGroup.HEAD)
            .unbreakable()
            .addItemFlag(ItemFlag.HIDE_DYE)
            .addItemFlag(ItemFlag.HIDE_ARMOR_TRIM);
    }),

    GROWTH_FARMER_LEGGINGS(Material.LEATHER_LEGGINGS, item -> {
        ItemMeta preMeta = item.getItemMeta();
        createGrowthItem(preMeta, "GROWTH_FARMER_LEGGINGS");
        item.setItemMeta(preMeta);
        new CustomItemBuilder(item, "Jambière du Fermier")
            .name("Jambière du Fermier 1", NamedTextColor.YELLOW)
            .leatherArmor(TrimMaterial.GOLD, TrimPattern.SILENCE, Color.fromRGB(16777045))
            .addAttribute(Attribute.ARMOR, -20, ADD_NUMBER, EquipmentSlotGroup.HEAD)
            .addAttribute(Attribute.MAX_HEALTH, 10, ADD_NUMBER, EquipmentSlotGroup.HEAD)
            .addAttribute(Attribute.MOVEMENT_SPEED, 0.03, ADD_NUMBER, EquipmentSlotGroup.HEAD) //évolutif
            .unbreakable()
            .addItemFlag(ItemFlag.HIDE_DYE)
            .addItemFlag(ItemFlag.HIDE_ARMOR_TRIM);
    }),

    GROWTH_ENCHANTER_HELMET(Material.LEATHER_HELMET, item -> {
        ItemMeta preMeta = item.getItemMeta();
        createGrowthItem(preMeta, "GROWTH_ENCHANTER_HELMET");
        item.setItemMeta(preMeta);
        new CustomItemBuilder(item, "Chapeau de l'enchanteur")
            .name("Chapeau de l'enchanteur 1", NamedTextColor.DARK_PURPLE)
            .leatherArmor(TrimMaterial.GOLD, TrimPattern.SILENCE, Color.fromRGB(11141290))
            .addAttribute(Attribute.ARMOR, -20, ADD_NUMBER, EquipmentSlotGroup.HEAD)
            .addAttribute(Attribute.MAX_HEALTH, 10, ADD_NUMBER, EquipmentSlotGroup.HEAD)
            .addAttribute(Attribute.MOVEMENT_SPEED, 0.03, ADD_NUMBER, EquipmentSlotGroup.HEAD)
            .unbreakable()
            .addItemFlag(ItemFlag.HIDE_DYE)
            .addItemFlag(ItemFlag.HIDE_ARMOR_TRIM);
    }),

    GROWTH_FISHERMAN_LEGGINGS(Material.LEATHER_LEGGINGS, item -> {
        ItemMeta preMeta = item.getItemMeta();
        createGrowthItem(preMeta, "GROWTH_FISHERMAN_LEGGINGS");
        item.setItemMeta(preMeta);
        new CustomItemBuilder(item, "Pantalon du pêcheur")
            .name("Pantalon du pêcheur 1", NamedTextColor.DARK_PURPLE)
            .leatherArmor(TrimMaterial.DIAMOND, TrimPattern.SILENCE, Color.fromRGB(5592575))
            .addAttribute(Attribute.ARMOR, -20, ADD_NUMBER, EquipmentSlotGroup.HEAD)
            .addAttribute(Attribute.MAX_HEALTH, -15, ADD_NUMBER, EquipmentSlotGroup.HEAD)
            .addAttribute(Attribute.MOVEMENT_SPEED, -0.06, ADD_NUMBER, EquipmentSlotGroup.HEAD)
            .unbreakable()
            .addItemFlag(ItemFlag.HIDE_DYE)
            .addItemFlag(ItemFlag.HIDE_ARMOR_TRIM);
    }),

    GROWTH_BOUSSOLE_EXPLORER(Material.COMPASS, item -> {
        ItemMeta preMeta = item.getItemMeta();
        createGrowthItem(preMeta, "GROWTH_BOUSSOLE_EXPLORER");
        item.setItemMeta(preMeta);
        new CustomItemBuilder(item, "GrowthBoussole")
                .name("Boussole de l'Explorateur I", NamedTextColor.AQUA)
                .addAttribute(Attribute.MOVEMENT_SPEED, 0.01, ADD_NUMBER, EquipmentSlotGroup.HAND)
                .unbreakable();
    }),


    FISHING_D_ROD(Material.FISHING_ROD, item -> {
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 5, true);
        meta.addEnchant(Enchantment.LURE, 5, true);
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 5, true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, false);
        item.setItemMeta(meta);
    }),

    AIR_FORCE(Material.LEATHER_BOOTS, item -> new CustomItemBuilder(item, "AirForce")
            .name("Air Force 1", NamedTextColor.YELLOW)
            .leatherArmor(TrimMaterial.NETHERITE, TrimPattern.SILENCE, Color.fromRGB(16383998))
            .addAttribute(Attribute.MOVEMENT_SPEED, 0.1, ADD_NUMBER, EquipmentSlotGroup.FEET)
            .addAttribute(Attribute.BLOCK_BREAK_SPEED, -0.8, ADD_SCALAR, EquipmentSlotGroup.FEET)
            .addAttribute(Attribute.ARMOR, -0.8, ADD_SCALAR, EquipmentSlotGroup.FEET)
            .unbreakable()
            .addItemFlag(ItemFlag.HIDE_DYE)
            .addItemFlag(ItemFlag.HIDE_ARMOR_TRIM)),


    ENDIALE(Material.LEATHER_CHESTPLATE, item -> new CustomItemBuilder(item, "AirForce")
            .name("Combinaison Endiale", NamedTextColor.YELLOW)
            .leatherArmor(TrimMaterial.AMETHYST, TrimPattern.SILENCE, Color.fromRGB(1408423))
            .addAttribute(Attribute.SCALE, -0.5, ADD_SCALAR, EquipmentSlotGroup.CHEST)
            .addAttribute(Attribute.BLOCK_BREAK_SPEED, -0.9, ADD_SCALAR, EquipmentSlotGroup.CHEST)
            .addAttribute(Attribute.MOVEMENT_SPEED, -0.02, ADD_NUMBER, EquipmentSlotGroup.CHEST)
            .unbreakable()
            .addItemFlag(ItemFlag.HIDE_DYE)
            .addItemFlag(ItemFlag.HIDE_ARMOR_TRIM)),

    TERMINATOR(Material.CROSSBOW, item -> {
        CrossbowMeta crossbowMeta = (CrossbowMeta) item.getItemMeta();
        crossbowMeta.customName(Component.text("Terminator", NamedTextColor.DARK_PURPLE));
        crossbowMeta.addEnchant(Enchantment.UNBREAKING, 3, false);
        crossbowMeta.addEnchant(Enchantment.MENDING, 1, false);
        crossbowMeta.addEnchant(Enchantment.PIERCING, 10, true);
        crossbowMeta.addEnchant(Enchantment.MULTISHOT, 1, true);
        crossbowMeta.addEnchant(Enchantment.QUICK_CHARGE, 100, true);
        item.setItemMeta(crossbowMeta);
    }),

    SEX_ON_THE_BEACH(Material.POTION, item -> {
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 900*20, 4), false);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 900*20, 1), false);
        meta.customName(Component.text("Sex On The Beatch", NamedTextColor.LIGHT_PURPLE));
        item.setItemMeta(meta);
    }),

    PORN_STAR_MARTINI(Material.SPLASH_POTION, item -> {
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 3), false);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.REGENERATION, 120*20, 1), false);
        meta.customName(Component.text("Porn Star Martini", NamedTextColor.LIGHT_PURPLE));
        item.setItemMeta(meta);
    }),

    SHOOTER_ORGASM(Material.POTION, item -> {
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.addCustomEffect(new PotionEffect(PotionEffectType.STRENGTH, 300*20, 2), false);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 300*20, 1), false);
        meta.customName(Component.text("Shooter Orgasm", NamedTextColor.LIGHT_PURPLE));
        item.setItemMeta(meta);
    }),

    GROWTH_PICKAXE(Material.WOODEN_PICKAXE, item -> {
        ItemMeta meta = item.getItemMeta();
        createGrowthItem(meta, "GROWTH_PICKAXE");
        meta.setUnbreakable(true);
        meta.customName(Component.text("The pickaxe", NamedTextColor.GOLD));
        item.setItemMeta(meta);
    }),

    GROWTH_SWORD(Material.WOODEN_SWORD, item -> {
        ItemMeta meta = item.getItemMeta();
        createGrowthItem(meta, "GROWTH_SWORD");
        meta.setUnbreakable(true);
        meta.customName(Component.text("The sword", NamedTextColor.GOLD));
        item.setItemMeta(meta);
    }),



    MUFFIN(Material.PLAYER_HEAD, item -> {
        SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer("Molflin"));
        skullMeta.customName(Component.text("Muffin", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(skullMeta);
    }),

    UNBREAKING3(Material.ENCHANTED_BOOK, item -> {
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
        meta.addStoredEnchant(Enchantment.UNBREAKING, 3, false);
        item.setItemMeta(meta);
    }),

    MENDING(Material.ENCHANTED_BOOK, item -> {
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
        meta.addStoredEnchant(Enchantment.MENDING, 1, false);
        item.setItemMeta(meta);
    }),

    CHANGER_ROLE(Material.POTION, item -> {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey(GameManager.getInstance().getPlugin(), "edible"), PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
    });

    // ─── Infrastructure enum ──────────────────────────────────────────────────

    private static final String DEFAULT_NAMESPACE = "survi";
    private static final Map<String, ECustomItem> LOOKUP = Arrays.stream(values()).collect(
            Collectors.toUnmodifiableMap(r -> r.name().toLowerCase(), r -> r));

    @Getter(lazy = true)
    private final NamespacedKey idKey = new NamespacedKey(GameManager.getInstance().getPlugin(), "unique_id");
    private final ItemStack itemStack;

    ECustomItem(Material material) {
        this(material, DEFAULT_NAMESPACE);
    }

    ECustomItem(Material material, ICustomItemMeta customItemMeta) {
        this(material);
        customItemMeta.ChangeItemMeta(this.itemStack);
    }

    ECustomItem(Material material, ICustomItemMeta customItemMeta, String uniqueId) {
        this(material, uniqueId);
        customItemMeta.ChangeItemMeta(this.itemStack);
    }

    ECustomItem(Material material, String uniqueId) {
        this.itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        meta.getPersistentDataContainer().set(getIdKey(), PersistentDataType.STRING, uniqueId);
        itemStack.setItemMeta(meta);
    }

    public static ECustomItem fromString(String input) {
        if (input == null) return null;
        return LOOKUP.get(input.toLowerCase());
    }

    public ItemStack getItemStack(int amount) {
        ItemStack item = this.getItemStack();
        item.setAmount(amount);
        return item;
    }

    private static void createGrowthItem(ItemMeta meta, String id) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(GrowthItems.ID_KEY, PersistentDataType.STRING, id);
        pdc.set(GrowthItems.USES_KEY, PersistentDataType.INTEGER, 0);
        pdc.set(GrowthItems.TIER_KEY, PersistentDataType.INTEGER, 0);
    }
}