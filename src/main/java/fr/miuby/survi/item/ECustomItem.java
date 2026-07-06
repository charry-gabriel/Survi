package fr.miuby.survi.item;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.growth_item.GrowthItems;
import fr.miuby.survi.quest.quest.QuestManager;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER;

@Getter
public enum ECustomItem {
    GROWTH_MINER_HELMET(Material.LEATHER_HELMET, item -> {
        ItemMeta preMeta = item.getItemMeta();
        createGrowthItem(preMeta, "GROWTH_MINER_HELMET");
        item.setItemMeta(preMeta);
        List<Component> list = new ArrayList<>();
        list.add(C.ARTIFACT_NAME);
        list.add(Component.empty());
        list.add(Component.text("Night_vison : x minerais", NamedTextColor.GRAY));
        new CustomItemBuilder(item, "growthMiner")
                .name("Casque du Mineur I", NamedTextColor.GRAY)
                .lore(list)
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

    GROWTH_LUMBERJACK_CHESTPLATE(Material.LEATHER_CHESTPLATE, item -> {
        ItemMeta preMeta = item.getItemMeta();
        createGrowthItem(preMeta, "GROWTH_LUMBERJACK_CHESTPLATE");
        item.setItemMeta(preMeta);
        List<Component> list = new ArrayList<>();
        list.add(C.ARTIFACT_NAME);
        list.add(Component.empty());
        list.add(Component.text("Speed : x bûches.", NamedTextColor.GRAY));
        new CustomItemBuilder(item, "growthLumberjack")
                .name("Plastron du Bûcheron I", NamedTextColor.DARK_GREEN)
                .lore(list)
                .leatherArmor(TrimMaterial.EMERALD, TrimPattern.DUNE, Color.fromRGB(43520))
                .addAttribute(Attribute.MINING_EFFICIENCY, 0, ADD_NUMBER, EquipmentSlotGroup.CHEST) //évolutif
                .addAttribute(Attribute.ARMOR, -20, ADD_NUMBER, EquipmentSlotGroup.CHEST)
                .addAttribute(Attribute.MAX_HEALTH, 10, ADD_NUMBER, EquipmentSlotGroup.CHEST)
                .addAttribute(Attribute.BLOCK_INTERACTION_RANGE, 2, ADD_NUMBER, EquipmentSlotGroup.CHEST)
                .addAttribute(Attribute.MOVEMENT_SPEED, -0.03, ADD_NUMBER, EquipmentSlotGroup.CHEST)
                .addAttribute(Attribute.SCALE, 0.3, ADD_NUMBER, EquipmentSlotGroup.CHEST)
                .unbreakable()
                .addItemFlag(ItemFlag.HIDE_DYE)
                .addItemFlag(ItemFlag.HIDE_ARMOR_TRIM);
    }),

    GROWTH_FARMER_LEGGINGS(Material.LEATHER_LEGGINGS, item -> {
        ItemMeta preMeta = item.getItemMeta();
        createGrowthItem(preMeta, "GROWTH_FARMER_LEGGINGS");
        item.setItemMeta(preMeta);
        List<Component> list = new ArrayList<>();
        list.add(C.ARTIFACT_NAME);
        list.add(Component.empty());
        list.add(Component.text("Saturation : x cultures", NamedTextColor.GRAY));
        new CustomItemBuilder(item, "growthFarmer")
                .name("Jambière du Fermier I", NamedTextColor.YELLOW)
                .lore(list)
                .leatherArmor(TrimMaterial.GOLD, TrimPattern.SILENCE, Color.fromRGB(16777045))
                .addAttribute(Attribute.ARMOR, -20, ADD_NUMBER, EquipmentSlotGroup.LEGS)
                .addAttribute(Attribute.MAX_HEALTH, -10, ADD_NUMBER, EquipmentSlotGroup.LEGS)
                .addAttribute(Attribute.MOVEMENT_SPEED, 0.03, ADD_NUMBER, EquipmentSlotGroup.LEGS) //évolutif
                .unbreakable()
                .addItemFlag(ItemFlag.HIDE_DYE)
                .addItemFlag(ItemFlag.HIDE_ARMOR_TRIM);
    }),

    GROWTH_ENCHANTER_HELMET(Material.LEATHER_HELMET, item -> {
        ItemMeta preMeta = item.getItemMeta();
        createGrowthItem(preMeta, "GROWTH_ENCHANTER_HELMET");
        item.setItemMeta(preMeta);
        List<Component> list = new ArrayList<>();
        list.add(C.ARTIFACT_NAME);
        list.add(Component.empty());
        list.add(Component.text("xp : x xp", NamedTextColor.GRAY));
        new CustomItemBuilder(item, "growthEnchanter")
                .name("Chapeau de l'enchanteur I", NamedTextColor.DARK_PURPLE)
                .lore(list)
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
        List<Component> list = new ArrayList<>();
        list.add(C.ARTIFACT_NAME);
        list.add(Component.empty());
        list.add(Component.text("Speed : x pêches", NamedTextColor.GRAY));
        new CustomItemBuilder(item, "growthFisherman")
                .name("Pantalon du pêcheur I", NamedTextColor.DARK_PURPLE)
                .lore(list)
                .leatherArmor(TrimMaterial.DIAMOND, TrimPattern.SILENCE, Color.fromRGB(5592575))
                .addAttribute(Attribute.LUCK, 0, ADD_NUMBER, EquipmentSlotGroup.LEGS) //évolutif
                .addAttribute(Attribute.ARMOR, -100, ADD_NUMBER, EquipmentSlotGroup.LEGS)
                .addAttribute(Attribute.ATTACK_DAMAGE, -100, ADD_NUMBER, EquipmentSlotGroup.LEGS)
                .addAttribute(Attribute.SCALE, 0.5, ADD_NUMBER, EquipmentSlotGroup.LEGS)
                .unbreakable()
                .addItemFlag(ItemFlag.HIDE_DYE)
                .addItemFlag(ItemFlag.HIDE_ARMOR_TRIM);
    }),

    GROWTH_EXPLORER_COMPASS(Material.COMPASS, item -> {
        ItemMeta preMeta = item.getItemMeta();
        createGrowthItem(preMeta, "GROWTH_EXPLORER_COMPASS");
        item.setItemMeta(preMeta);
        List<Component> list = new ArrayList<>();
        list.add(C.ARTIFACT_NAME);
        list.add(Component.empty());
        list.add(Component.text("Speed : x biomes", NamedTextColor.GRAY));
        new CustomItemBuilder(item, "growthCompass")
                .name("Boussole de l'Explorateur I", NamedTextColor.AQUA)
                .lore(list)
                .addAttribute(Attribute.MOVEMENT_SPEED, 0, ADD_NUMBER, EquipmentSlotGroup.HAND) //évolutif
                .unbreakable();
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
    }),

    QUEST_REROLL(Material.POTION, item -> {
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setColor(Color.fromRGB(0xE8A33D));
        meta.getPersistentDataContainer().set(QuestManager.QUEST_REROLL_KEY, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Annule votre quête en cours pour en accepter une nouvelle.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Limite : 1 par jour", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        new CustomItemBuilder(item, "questReroll")
                .name("Fiole de Reroll", NamedTextColor.GOLD)
                .lore(lore);
    }),

    BACKPACK(Material.LEATHER, item -> {
        ItemMeta preMeta = item.getItemMeta();
        preMeta.getPersistentDataContainer().set(BackpackService.BACKPACK_MARKER_KEY, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(preMeta);
        List<Component> list = new ArrayList<>();
        list.add(Component.text("Unique", NamedTextColor.YELLOW));
        new CustomItemBuilder(item, "backpack")
                .name("Sac du bûcheron", NamedTextColor.DARK_GREEN)
                .lore(list)
                .itemModel(new NamespacedKey("survi", "backpack"))
                .maxStackSize(1)
                .unbreakable();
    }),

    // ─── Objets rares de collection (un par métier) ──────────────────────────

    RARE_MINER(Material.LEATHER, item -> {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Collection · Mineur", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Unique", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        new CustomItemBuilder(item, "rareMiner")
                .name("Fragment du Mineur Légendaire", NamedTextColor.GRAY)
                .lore(lore)
                .itemModel(new NamespacedKey("survi", "rare_miner"))
                .maxStackSize(1);
    }, "rareMiner"),

    RARE_LUMBERJACK(Material.LEATHER, item -> {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Collection · Bûcheron", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Unique", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        new CustomItemBuilder(item, "rareLumberjack")
                .name("Éclat du Bûcheron Légendaire", NamedTextColor.DARK_GREEN)
                .lore(lore)
                .itemModel(new NamespacedKey("survi", "rare_lumberjack"))
                .maxStackSize(1);
    }, "rareLumberjack"),

    RARE_FARMER(Material.LEATHER, item -> {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Collection · Fermier", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Unique", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        new CustomItemBuilder(item, "rareFarmer")
                .name("Graine du Fermier Légendaire", NamedTextColor.YELLOW)
                .lore(lore)
                .itemModel(new NamespacedKey("survi", "rare_farmer"))
                .maxStackSize(1);
    }, "rareFarmer"),

    RARE_ENCHANTER(Material.LEATHER, item -> {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Collection · Enchanteur", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Unique", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        new CustomItemBuilder(item, "rareEnchanter")
                .name("Larme de l'Enchanteur Légendaire", NamedTextColor.DARK_PURPLE)
                .lore(lore)
                .itemModel(new NamespacedKey("survi", "rare_enchanter"))
                .maxStackSize(1);
    }, "rareEnchanter"),

    RARE_FISHERMAN(Material.LEATHER, item -> {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Collection · Pêcheur", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Unique", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        new CustomItemBuilder(item, "rareFisherman")
                .name("Coquille du Pêcheur Légendaire", NamedTextColor.AQUA)
                .lore(lore)
                .itemModel(new NamespacedKey("survi", "rare_fisherman"))
                .maxStackSize(1);
    }, "rareFisherman"),

    RARE_EXPLORER(Material.LEATHER, item -> {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Collection · Explorateur", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Unique", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        new CustomItemBuilder(item, "rareExplorer")
                .name("Fragment de l'Explorateur Légendaire", NamedTextColor.RED)
                .lore(lore)
                .itemModel(new NamespacedKey("survi", "rare_explorer"))
                .maxStackSize(1);
    }, "rareExplorer"),

    // ─── Indices vers les objets rares (un livre par métier) ──────────────

    HINT_BOOK_MINER(Material.WRITTEN_BOOK, item -> {
        BookMeta meta = (BookMeta) item.getItemMeta();
        meta.title(Component.text("Le Secret du Mineur"));
        meta.author(Component.text("Team Alpha"));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Indice", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Un trésor légendaire attend d'être découvert...", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.addPages(
                Component.text("Il existe, dit-on, un trésor que nulle forge n'a jamais façonné — et ce n'est pas le seul de son espèce. Chaque métier cache le sien, une rareté que la plupart ne croiseront jamais, même après une vie entière."),
                Component.text("Le secret se gagne minerai après minerai, veine après veine, sans jamais reposer la pioche. Seule la constance compte, où que tu frappes la roche.")
        );
        item.setItemMeta(meta);
    }),

    HINT_BOOK_LUMBERJACK(Material.WRITTEN_BOOK, item -> {
        BookMeta meta = (BookMeta) item.getItemMeta();
        meta.title(Component.text("Le Secret du Bûcheron"));
        meta.author(Component.text("Team Alpha"));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Indice", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Un trésor légendaire attend d'être découvert...", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.addPages(
                Component.text("Au cœur des forêts les plus anciennes, une légende circule : chaque métier cacherait un trésor qui lui est propre, et celui du bûcheron attendrait quelque part, sous l'écorce la plus ancienne. Peu y croient. Moins encore le trouvent."),
                Component.text("Ce n'est pas la hache qui choisit, mais la constance. Tronc après tronc, abattus sans relâche, le bois finit parfois par livrer un secret bien plus précieux.")
        );
        item.setItemMeta(meta);
    }),

    HINT_BOOK_FARMER(Material.WRITTEN_BOOK, item -> {
        BookMeta meta = (BookMeta) item.getItemMeta();
        meta.title(Component.text("Le Secret du Fermier"));
        meta.author(Component.text("Team Alpha"));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Indice", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Un trésor légendaire attend d'être découvert...", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.addPages(
                Component.text("Chaque récolte semble ordinaire... jusqu'au jour où elle ne l'est plus. On raconte que chaque métier cache un trésor qui lui est propre, et que celui des champs n'attend que les plus patients."),
                Component.text("Sème, entretiens, récolte — encore et encore. La terre ne se presse jamais, mais elle se souvient de ceux qui ne renoncent pas.")
        );
        item.setItemMeta(meta);
    }),

    HINT_BOOK_ENCHANTER(Material.WRITTEN_BOOK, item -> {
        BookMeta meta = (BookMeta) item.getItemMeta();
        meta.title(Component.text("Le Secret de l'Enchanteur"));
        meta.author(Component.text("Team Alpha"));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Indice", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Un trésor légendaire attend d'être découvert...", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.addPages(
                Component.text("Entre les pages des grimoires circule une rumeur : chaque métier cache un objet d'exception qui lui est propre. Celui de l'enchanteur est une rareté que nulle table n'a jamais vue deux fois."),
                Component.text("Chaque sort tissé sur ta table tisse aussi autre chose, d'invisible. Continue d'enchanter, encore et encore, et peut-être le voile se lèvera.")
        );
        item.setItemMeta(meta);
    }),

    HINT_BOOK_FISHERMAN(Material.WRITTEN_BOOK, item -> {
        BookMeta meta = (BookMeta) item.getItemMeta();
        meta.title(Component.text("Le Secret du Pêcheur"));
        meta.author(Component.text("Team Alpha"));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Indice", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Un trésor légendaire attend d'être découvert...", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.addPages(
                Component.text("Dans les tavernes du port, les vieux pêcheurs murmurent que chaque métier cache un trésor qui lui est propre. Celui du pêcheur : une prise unique au monde, pour celui qui ne remontera jamais sa ligne."),
                Component.text("Lance ta ligne, encore et encore, par tous les temps. Ce n'est jamais le même poisson qui mord deux fois... et parfois, ce n'est pas un poisson du tout.")
        );
        item.setItemMeta(meta);
    }),

    HINT_BOOK_EXPLORER(Material.WRITTEN_BOOK, item -> {
        BookMeta meta = (BookMeta) item.getItemMeta();
        meta.title(Component.text("Le Secret de l'Explorateur"));
        meta.author(Component.text("Team Alpha"));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Indice", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Un trésor légendaire attend d'être découvert...", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.addPages(
                Component.text("Au-delà du portail, dans la Wilderness où peu osent s'aventurer, une rumeur circule : chaque métier cache un trésor qui lui est propre, et celui de l'explorateur y attend, plus légendaire qu'aucun trophée de chasse."),
                Component.text("Il se gagne l'épée à la main, loin dans le Wilderness, en terrassant ses créatures les plus féroces, encore et encore, jusqu'à ce que la légende cède enfin son secret.")
        );
        item.setItemMeta(meta);
    });

    // ─── Infrastructure enum ──────────────────────────────────────────────────

    private interface C {
        TextComponent ARTIFACT_NAME = Component.text("Artéfact des Maîtres",
                Style.style(NamedTextColor.GOLD)
                        .decoration(TextDecoration.BOLD, true)
                        .decoration(TextDecoration.ITALIC, false));
    }

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