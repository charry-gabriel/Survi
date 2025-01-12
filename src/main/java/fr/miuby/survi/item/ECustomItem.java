package fr.miuby.survi.item;

import fr.miuby.survi.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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

    SEX_ON_THE_BEACH(Material.POTION, itemStack -> {
        PotionMeta meta = (PotionMeta) itemStack.getItemMeta();
        meta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 900*20, 4), false);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 900*20, 1), false);
        itemStack.setItemMeta(meta);
    }),

    PORN_STAR_MARTINI(Material.POTION, itemStack -> {
        PotionMeta meta = (PotionMeta) itemStack.getItemMeta();
        meta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 3), false);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.REGENERATION, 120*20, 1), false);
        itemStack.setItemMeta(meta);
    }),

    SHOOTER_ORGASM(Material.POTION, itemStack -> {
        PotionMeta meta = (PotionMeta) itemStack.getItemMeta();
        meta.addCustomEffect(new PotionEffect(PotionEffectType.STRENGTH, 300*20, 2), false);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 300*20, 1), false);
        itemStack.setItemMeta(meta);
    }),

    SPICY_SWEET_DREAMS_TICKET(Material.NAME_TAG, "spicy"),

    CLE1(Material.NAME_TAG, "cle1"),

    CLE2(Material.NAME_TAG, "cle2"),

    MENDING(Material.ENCHANTED_BOOK, itemStack -> {
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemStack.getItemMeta();
        meta.addStoredEnchant(Enchantment.MENDING, 1, false);
        itemStack.setItemMeta(meta);
    });

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
        meta.getPersistentDataContainer().set(new NamespacedKey(GameManager.getInstance().getPlugin(), "unique_id"), PersistentDataType.STRING, uniqueId);
        itemStack.setItemMeta(meta);
    }

    public ItemStack getItemStack() {
        return itemStack;
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
}
