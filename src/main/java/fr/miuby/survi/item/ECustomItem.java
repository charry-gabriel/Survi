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
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Getter
@SuppressWarnings("UnstableApiUsage")
public enum ECustomItem {
    FISHING_D_ROD(Material.FISHING_ROD, itemStack -> {
        ItemMeta meta = itemStack.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 5, true);
        meta.addEnchant(Enchantment.LURE, 5, true);
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 5, true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, false);
        itemStack.setItemMeta(meta);
    }),

    AIR_FORCE(Material.LEATHER_BOOTS, ECustomItem::getAirForce),

    MINEUR(Material.LEATHER_HELMET, ECustomItem::getMineur),

    ENDIALE(Material.LEATHER_CHESTPLATE, ECustomItem::getEndiale),

    TERMINATOR(Material.CROSSBOW, ECustomItem::getTerminator),

    SEX_ON_THE_BEACH(Material.POTION, itemStack -> {
        PotionMeta meta = (PotionMeta) itemStack.getItemMeta();
        meta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 900*20, 4), false);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 900*20, 1), false);
        meta.customName(Component.text("Sex On The Beatch", NamedTextColor.LIGHT_PURPLE));
        itemStack.setItemMeta(meta);
    }),

    PORN_STAR_MARTINI(Material.SPLASH_POTION, itemStack -> {
        PotionMeta meta = (PotionMeta) itemStack.getItemMeta();
        meta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 3), false);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.REGENERATION, 120*20, 1), false);
        meta.customName(Component.text("Porn Star Martini", NamedTextColor.LIGHT_PURPLE));
        itemStack.setItemMeta(meta);
    }),

    SHOOTER_ORGASM(Material.POTION, itemStack -> {
        PotionMeta meta = (PotionMeta) itemStack.getItemMeta();
        meta.addCustomEffect(new PotionEffect(PotionEffectType.STRENGTH, 300*20, 2), false);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 300*20, 1), false);
        meta.customName(Component.text("Shooter Orgasm", NamedTextColor.LIGHT_PURPLE));
        itemStack.setItemMeta(meta);
    }),

    HEALING_ARROW(Material.TIPPED_ARROW, itemStack -> {
        PotionMeta meta = (PotionMeta) itemStack.getItemMeta();
        meta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 1), false);
        meta.customName(Component.text("JE TE HEAL", NamedTextColor.DARK_RED));
        itemStack.setItemMeta(meta);
    }),

    SPICY_SWEET_DREAMS_TICKET(Material.NAME_TAG, itemStack -> {
        ItemMeta meta = itemStack.getItemMeta();
        meta.customName(Component.text("Spicy Sweet Dreams Ticket", NamedTextColor.LIGHT_PURPLE));
        itemStack.setItemMeta(meta);
    }),

    CLE00(Material.NAME_TAG, itemStack -> {
        ItemMeta meta = itemStack.getItemMeta();
        meta.customName(Component.text("Clé 00", NamedTextColor.GOLD));
        meta.setItemModel(new NamespacedKey(GameManager.getInstance().getPlugin(), "key"));
        itemStack.setItemMeta(meta);
    }, "cle00"),
    CLE01(Material.NAME_TAG, itemStack -> {
        ItemMeta meta = itemStack.getItemMeta();
        meta.customName(Component.text("Clé 01", NamedTextColor.GOLD));
        meta.setItemModel(new NamespacedKey(GameManager.getInstance().getPlugin(), "key"));
        itemStack.setItemMeta(meta);
    }, "cle01"),
    CLE02(Material.NAME_TAG, itemStack -> {
        ItemMeta meta = itemStack.getItemMeta();
        meta.customName(Component.text("Clé 02", NamedTextColor.GOLD));
        meta.setItemModel(new NamespacedKey(GameManager.getInstance().getPlugin(), "key"));
        itemStack.setItemMeta(meta);
    }, "cle02"),
    CLE11(Material.NAME_TAG, itemStack -> {
        ItemMeta meta = itemStack.getItemMeta();
        meta.customName(Component.text("Clé 11", NamedTextColor.GOLD));
        meta.setItemModel(new NamespacedKey(GameManager.getInstance().getPlugin(), "key"));
        itemStack.setItemMeta(meta);
    }, "cle11"),
    CLE12(Material.NAME_TAG, itemStack -> {
        ItemMeta meta = itemStack.getItemMeta();
        meta.customName(Component.text("Clé 12", NamedTextColor.GOLD));
        meta.setItemModel(new NamespacedKey(GameManager.getInstance().getPlugin(), "key"));
        itemStack.setItemMeta(meta);
    }, "cle12"),
    CLE13(Material.NAME_TAG, itemStack -> {
        ItemMeta meta = itemStack.getItemMeta();
        meta.customName(Component.text("Clé 13", NamedTextColor.GOLD));
        meta.setItemModel(new NamespacedKey(GameManager.getInstance().getPlugin(), "key"));
        itemStack.setItemMeta(meta);
    }, "cle13"),
    CLE14(Material.NAME_TAG, itemStack -> {
        ItemMeta meta = itemStack.getItemMeta();
        meta.customName(Component.text("Clé 14", NamedTextColor.GOLD));
        meta.setItemModel(new NamespacedKey(GameManager.getInstance().getPlugin(), "key"));
        itemStack.setItemMeta(meta);
    }, "cle14"),
    CLE15(Material.NAME_TAG, itemStack -> {
        ItemMeta meta = itemStack.getItemMeta();
        meta.customName(Component.text("Clé 15", NamedTextColor.GOLD));
        meta.setItemModel(new NamespacedKey(GameManager.getInstance().getPlugin(), "key"));
        itemStack.setItemMeta(meta);
    }, "cle15"),
    CLE16(Material.NAME_TAG, itemStack -> {
        ItemMeta meta = itemStack.getItemMeta();
        meta.customName(Component.text("Clé 16", NamedTextColor.GOLD));
        meta.setItemModel(new NamespacedKey(GameManager.getInstance().getPlugin(), "key"));
        itemStack.setItemMeta(meta);
    }, "cle16"),
    CLE31(Material.NAME_TAG, itemStack -> {
        ItemMeta meta = itemStack.getItemMeta();
        meta.customName(Component.text("Clé 31", NamedTextColor.GOLD));
        meta.setItemModel(new NamespacedKey(GameManager.getInstance().getPlugin(), "key"));
        itemStack.setItemMeta(meta);
    }, "cle31"),
    CLE32(Material.NAME_TAG, itemStack -> {
        ItemMeta meta = itemStack.getItemMeta();
        meta.customName(Component.text("Clé 32", NamedTextColor.GOLD));
        meta.setItemModel(new NamespacedKey(GameManager.getInstance().getPlugin(), "key"));
        itemStack.setItemMeta(meta);
    }, "cle32"),

    GROWTH_PICKAXE(Material.WOODEN_PICKAXE, itemStack -> {
        ItemMeta meta = itemStack.getItemMeta();
        CreateGrowthItem(meta, "GROWTH_PICKAXE");

        meta.setUnbreakable(true);
        meta.customName(Component.text("The pickaxe", NamedTextColor.GOLD));
        //meta.setItemModel(new NamespacedKey(GameManager.getInstance().getPlugin(), "pickaxe"));
        itemStack.setItemMeta(meta);
    }),

    GROWTH_SWORD(Material.WOODEN_SWORD, itemStack -> {
        ItemMeta meta = itemStack.getItemMeta();
        CreateGrowthItem(meta, "GROWTH_SWORD");

        meta.setUnbreakable(true);
        meta.customName(Component.text("The sword", NamedTextColor.GOLD));
        //meta.setItemModel(new NamespacedKey(GameManager.getInstance().getPlugin(), "pickaxe"));
        itemStack.setItemMeta(meta);
    }),


    MUFFIN(Material.PLAYER_HEAD, itemStack -> {
        SkullMeta skullMeta = (SkullMeta) itemStack.getItemMeta();
        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer("Molflin"));
        skullMeta.customName(Component.text("Muffin", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        itemStack.setItemMeta(skullMeta);
    }),

    UNBREAKING3(Material.ENCHANTED_BOOK, itemStack -> {
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemStack.getItemMeta();
        meta.addStoredEnchant(Enchantment.UNBREAKING, 3, false);
        itemStack.setItemMeta(meta);
    }),

    MENDING(Material.ENCHANTED_BOOK, itemStack -> {
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemStack.getItemMeta();
        meta.addStoredEnchant(Enchantment.MENDING, 1, false);
        itemStack.setItemMeta(meta);
    });


    public final NamespacedKey ID_KEY = new NamespacedKey(GameManager.getInstance().getPlugin(), "unique_id");
    private final ItemStack itemStack;

    ECustomItem(Material material) {
        this(material, "survi");
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
        meta.getPersistentDataContainer().set(ID_KEY, PersistentDataType.STRING, uniqueId);
        itemStack.setItemMeta(meta);
    }

    public ItemStack getItemStack(int amount) {
        ItemStack itemStack = this.getItemStack();
        itemStack.setAmount(amount);
        return itemStack;
    }

    private static void CreateGrowthItem(ItemMeta meta, String id) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(GrowthItems.ID_KEY, PersistentDataType.STRING, id);
        pdc.set(GrowthItems.USES_KEY, PersistentDataType.INTEGER, 0);
        pdc.set(GrowthItems.TIER_KEY, PersistentDataType.INTEGER, 0);
    }

    private static void getAirForce(ItemStack itemStack) {
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
    }

    private static void getMineur(ItemStack itemStack) {
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
    }

    private static void getEndiale(ItemStack itemStack) {
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
    }

    private static void getTerminator(ItemStack itemStack) {
        CrossbowMeta crossbowMeta = (CrossbowMeta) itemStack.getItemMeta();
        crossbowMeta.customName(Component.text("Terminator", NamedTextColor.DARK_PURPLE));
        crossbowMeta.addEnchant(Enchantment.UNBREAKING, 3, false);
        crossbowMeta.addEnchant(Enchantment.MENDING, 1, false);
        crossbowMeta.addEnchant(Enchantment.PIERCING, 10, true);
        crossbowMeta.addEnchant(Enchantment.MULTISHOT, 1, true);
        crossbowMeta.addEnchant(Enchantment.QUICK_CHARGE, 100, true);
        itemStack.setItemMeta(crossbowMeta);
    }
}
