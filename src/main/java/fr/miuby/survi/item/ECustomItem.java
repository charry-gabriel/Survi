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

    MINEUR(Material.LEATHER_HELMET, item -> new CustomItemBuilder(item, "AirForce")
            .name("Casque de Mineur", NamedTextColor.YELLOW)
            .leatherArmor(TrimMaterial.GOLD, TrimPattern.FLOW, Color.fromRGB(13061821))
            .addAttribute(Attribute.MINING_EFFICIENCY, 10, ADD_NUMBER, EquipmentSlotGroup.HEAD)
            .addAttribute(Attribute.MOVEMENT_SPEED, -0.02, ADD_NUMBER, EquipmentSlotGroup.HEAD)
            .addAttribute(Attribute.ARMOR, -0.8, ADD_SCALAR, EquipmentSlotGroup.HEAD)
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

    HEALING_ARROW(Material.TIPPED_ARROW, item -> {
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 1), false);
        meta.customName(Component.text("JE TE HEAL", NamedTextColor.DARK_RED));
        item.setItemMeta(meta);
    }),

    SPICY_SWEET_DREAMS_TICKET(Material.NAME_TAG, item -> {
        ItemMeta meta = item.getItemMeta();
        meta.customName(Component.text("Spicy Sweet Dreams Ticket", NamedTextColor.LIGHT_PURPLE));
        item.setItemMeta(meta);
    }),

    CLE00(Material.NAME_TAG, createKey("00"), "cle00"),
    CLE01(Material.NAME_TAG, createKey("01"), "cle01"),
    CLE02(Material.NAME_TAG, createKey("02"), "cle02"),
    CLE11(Material.NAME_TAG, createKey("11"), "cle11"),
    CLE12(Material.NAME_TAG, createKey("12"), "cle12"),
    CLE13(Material.NAME_TAG, createKey("13"), "cle13"),
    CLE14(Material.NAME_TAG, createKey("14"), "cle14"),
    CLE15(Material.NAME_TAG, createKey("15"), "cle15"),
    CLE16(Material.NAME_TAG, createKey("16"), "cle16"),
    CLE31(Material.NAME_TAG, createKey("31"), "cle31"),
    CLE32(Material.NAME_TAG, createKey("32"), "cle32"),

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

    /**
     * Casque de mineur growth item (MINEUR, niveau 4+).
     *
     * <p>Item secondaire du métier MINEUR : porté sur la tête, il grandit en minant
     * des minerais (OreBreakEvent dans GrowthItemListener) et gagne progressivement
     * en efficacité minière via {@code SetAttributeItemEffect}.
     *
     * <p>L'avantage par rapport à GROWTH_PICKAXE : la pioche reste totalement libre
     * — le joueur peut l'enchanter normalement (Fortune III, Efficiency V, etc.)
     * sans qu'elle soit "consommée" par le système de growth.
     *
     * <p>Attribut initial : MINING_EFFICIENCY +3 (remplacé aux paliers 15/35/65 minerais
     * par +8, +14, +22 via SetAttributeItemEffect qui efface les anciens modificateurs).
     */
    GROWTH_CASQUE_MINEUR(Material.LEATHER_HELMET, item -> {
        // Étape 1 : PDC growth item (doit précéder CustomItemBuilder pour que la meta soit commitée)
        ItemMeta preMeta = item.getItemMeta();
        createGrowthItem(preMeta, "GROWTH_CASQUE_MINEUR");
        item.setItemMeta(preMeta);
        // Étape 2 : apparence + attribut initial de mining efficiency
        // Le CustomItemBuilder relit la meta déjà commitée → le PDC est préservé
        new CustomItemBuilder(item, "GrowthCasque")
                .name("Casque de Mineur I", NamedTextColor.GOLD)
                .leatherArmor(TrimMaterial.GOLD, TrimPattern.FLOW, Color.fromRGB(0xC8960A)) // doré ambré
                .addAttribute(Attribute.MINING_EFFICIENCY, 3.0, ADD_NUMBER, EquipmentSlotGroup.HEAD)
                .addAttribute(Attribute.MOVEMENT_SPEED, -0.01, ADD_NUMBER, EquipmentSlotGroup.HEAD)
                .unbreakable()
                .addItemFlag(ItemFlag.HIDE_DYE)
                .addItemFlag(ItemFlag.HIDE_ARMOR_TRIM);
    }),

    /**
     * Bâton de récolte growth item (FERMIER, niveau 4+).
     *
     * <p>Item secondaire du métier FERMIER : tenu en main secondaire, il grandit en
     * cassant des cultures matures (CropBreakEvent dans GrowthItemListener) et augmente
     * le rendement de récolte via un multiplicateur de drops géré dans le listener.
     *
     * <p>La houe reste totalement libre en main principale — le bâton ne l'encombre pas.
     * Multiplicateur de drops bonus : tier × 0,5 (tier 1 = +50 %, tier 2 = +100 %, tier 3 = +150 %).
     */
    GROWTH_BATON_FERMIER(Material.BLAZE_ROD, item -> {
        ItemMeta meta = item.getItemMeta();
        createGrowthItem(meta, "GROWTH_BATON_FERMIER");
        meta.setUnbreakable(true);
        meta.customName(Component.text("Bâton du Fermier I", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
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

    private static ICustomItemMeta createKey(String number) {
        return itemStack -> {
            ItemMeta meta = itemStack.getItemMeta();
            meta.customName(Component.text("Clé " + number, NamedTextColor.GOLD));
            meta.setItemModel(new NamespacedKey(GameManager.getInstance().getPlugin(), "key"));
            itemStack.setItemMeta(meta);
        };
    }
}