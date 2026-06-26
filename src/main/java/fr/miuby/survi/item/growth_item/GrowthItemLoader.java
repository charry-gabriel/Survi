package fr.miuby.survi.item.growth_item;

import fr.miuby.lib.resource.MLResourceManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.growth_item.config.GrowthConfig;
import fr.miuby.survi.item.growth_item.config.GrowthItemFileConfig;
import fr.miuby.survi.item.growth_item.config.GrowthItemFileConfig.EffectConfig;
import fr.miuby.survi.item.growth_item.effect.*;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.lib.log.MLLogManager;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.logging.Level;

public final class GrowthItemLoader {

    private GrowthItemLoader() {}

    /**
     * Compteur monotone incrémenté à chaque {@link #reload()}, persisté dans {@code server_data}
     * (clé {@link #SERVER_DATA_VERSION_KEY}) pour survivre à un redémarrage du serveur.
     *
     * <p>Comparé à la clé PDC {@code growth_reload_version} portée par chaque item pour détecter
     * les items qui n'ont pas encore intégré la nouvelle config (voir {@link GrowthItems#checkAndReapplyIfStale}).
     * Cette persistance est nécessaire : la clé PDC, elle, survit aux redémarrages (sauvegardée avec
     * l'item). Si {@code configVersion} repartait de 0 en mémoire à chaque démarrage alors qu'un item
     * porte déjà une version supérieure issue d'une session précédente, l'item serait considéré comme
     * à jour à tort et ne profiterait plus jamais de {@code reapplyAll}.</p>
     */
    @Getter
    private static int configVersion = 0;

    private static final String SERVER_DATA_VERSION_KEY = "growth_items_config_version";

    // =========================================================================
    // Chargement
    // =========================================================================

    /**
     * Restaure {@link #configVersion} depuis {@code server_data} au démarrage du plugin.
     * À appeler une seule fois, avant le premier {@link #loadAll()} (voir {@link GrowthItems#init()}).
     */
    public static void initConfigVersion() {
        String raw = GameManager.getInstance().getDatabase().system().getServerData(SERVER_DATA_VERSION_KEY);
        try {
            configVersion = raw != null ? Integer.parseInt(raw) : 0;
        } catch (NumberFormatException e) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.ITEM,
                    "[GrowthItemLoader] Valeur de '" + SERVER_DATA_VERSION_KEY + "' invalide en base ('"
                            + raw + "'), configVersion réinitialisé à 0");
            configVersion = 0;
        }
        MLLogManager.getInstance().log(Level.INFO, ELogTag.ITEM,
                "[GrowthItemLoader] configVersion restauré depuis server_data : " + configVersion);
    }

    public static void loadAll() {
        List<GrowthItemFileConfig> files = MLResourceManager.loadPojoAll(
                GameManager.getInstance().getPlugin(), "growth_items", GrowthItemFileConfig.class);

        for (GrowthItemFileConfig cfg : files) {
            try {
                GrowthConfig growthConfig = convert(cfg);
                GrowthItemRegistry.register(cfg.id, growthConfig);
                MLLogManager.getInstance().log(Level.INFO, ELogTag.ITEM,
                        "Growth item chargé : " + cfg.id + " (" + cfg.tiers.size() + " paliers)");
            } catch (Exception e) {
                MLLogManager.getInstance().log(Level.SEVERE, ELogTag.ITEM,
                        "Erreur lors du chargement du growth item '" + cfg.id + "' : " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // Reload à chaud
    // =========================================================================

    /**
     * Recharge tous les fichiers {@code growth_items/*.yml} à chaud, sans redémarrage.
     *
     * <p>Séquence :</p>
     * <ol>
     *   <li>Vide {@link GrowthItemRegistry}.</li>
     *   <li>Invalide le cache {@link MLResourceManager}.</li>
     *   <li>Relit tous les fichiers et repeuple le registre.</li>
     *   <li>Incrémente {@link #configVersion} et le persiste en base — les items growth qui ne
     *       l'ont pas encore intégré seront mis à jour paresseusement dès qu'ils seront tenus
     *       en main ou équipés (voir {@link GrowthItems#checkAndReapplyIfStale}).</li>
     * </ol>
     */
    public static void reload() {
        GrowthItemRegistry.clear();
        MLResourceManager.clearCache();
        loadAll();
        configVersion++;
        GameManager.getInstance().getDatabase().system().saveServerData(
                SERVER_DATA_VERSION_KEY, String.valueOf(configVersion));
        MLLogManager.getInstance().log(Level.INFO, ELogTag.ITEM,
                "[GrowthItemLoader] Reload effectué — configVersion=" + configVersion + " (persisté)");
    }

    // ─── Conversion ───────────────────────────────────────────────────────────

    private static GrowthConfig convert(GrowthItemFileConfig cfg) {
        GrowthConfig.Builder builder = GrowthConfig.builder(cfg.eventType);
        if (cfg.baseEffects != null && !cfg.baseEffects.isEmpty())
            builder.baseEffects(convertEffects(cfg.baseEffects));
        for (var tier : cfg.tiers)
            builder.tier(tier.requiredUses, convertEffects(tier.effects));
        for (var periodic : cfg.periodicEffects)
            builder.periodic(periodic.everyUses, convertEffects(periodic.effects));
        return builder.build();
    }

    private static List<ItemEffect> convertEffects(List<EffectConfig> entries) {
        return entries.stream().map(GrowthItemLoader::convertEffect).toList();
    }

    private static ItemEffect convertEffect(EffectConfig e) {
        return switch (e.type) {
            case "name"             -> new NameItemEffect(e.value);
            case "message"          -> new MessageItemEffect(Component.text(e.value));
            case "potion"           -> new PotionItemEffect(parsePotionEffect(e.effect), e.seconds, e.amplifier);
            case "fire_enemies"     -> new FireEnemiesItemEffect(e.seconds);
            case "unlock_ability"   -> new UnlockAbilityItemEffect(e.value);
            case "add_enchantment"  -> new AddEnchantmentItemEffect(parseEnchantment(e.enchantment), e.amount);
            case "set_attribute"    -> new SetAttributeItemEffect(
                    parseAttribute(e.attribute), e.attributeValue,
                    parseOperation(e.operation), parseSlotGroup(e.slot));
            default -> throw new IllegalArgumentException("Type d'effet inconnu : '" + e.type + "'");
        };
    }

    // ─── Parseurs ─────────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private static Enchantment parseEnchantment(String name) {
        Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(name.toLowerCase()));
        if (ench == null)
            throw new IllegalArgumentException("Enchantement inconnu : '" + name + "'");
        return ench;
    }

    private static Attribute parseAttribute(String name) {
        return switch (name.toLowerCase()) {
            case "mining_efficiency"    -> Attribute.MINING_EFFICIENCY;
            case "movement_speed"       -> Attribute.MOVEMENT_SPEED;
            case "attack_damage"        -> Attribute.ATTACK_DAMAGE;
            case "attack_speed"         -> Attribute.ATTACK_SPEED;
            case "armor"                -> Attribute.ARMOR;
            case "armor_toughness"      -> Attribute.ARMOR_TOUGHNESS;
            case "max_health"           -> Attribute.MAX_HEALTH;
            case "block_break_speed"    -> Attribute.BLOCK_BREAK_SPEED;
            case "luck"                 -> Attribute.LUCK;
            case "knockback_resistance" -> Attribute.KNOCKBACK_RESISTANCE;
            default -> throw new IllegalArgumentException(
                    "Attribut inconnu : '" + name + "'. Ajouter le case dans GrowthItemLoader.parseAttribute()");
        };
    }

    @SuppressWarnings("deprecation")
    private static PotionEffectType parsePotionEffect(String name) {
        PotionEffectType type = PotionEffectType.getByName(name.toUpperCase());
        if (type == null)
            throw new IllegalArgumentException(
                    "PotionEffectType inconnu : '" + name + "' (ex. speed, strength, night_vision)");
        return type;
    }

    private static AttributeModifier.Operation parseOperation(String name) {
        return switch (name.toUpperCase()) {
            case "ADD_NUMBER"        -> AttributeModifier.Operation.ADD_NUMBER;
            case "ADD_SCALAR"        -> AttributeModifier.Operation.ADD_SCALAR;
            case "MULTIPLY_SCALAR_1" -> AttributeModifier.Operation.MULTIPLY_SCALAR_1;
            default -> throw new IllegalArgumentException("Opération inconnue : '" + name + "'");
        };
    }

    private static EquipmentSlotGroup parseSlotGroup(String name) {
        return switch (name.toUpperCase()) {
            case "HEAD"    -> EquipmentSlotGroup.HEAD;
            case "CHEST"   -> EquipmentSlotGroup.CHEST;
            case "LEGS"    -> EquipmentSlotGroup.LEGS;
            case "FEET"    -> EquipmentSlotGroup.FEET;
            case "HAND"    -> EquipmentSlotGroup.HAND;
            case "OFFHAND" -> EquipmentSlotGroup.OFFHAND;
            case "ARMOR"   -> EquipmentSlotGroup.ARMOR;
            case "ANY"     -> EquipmentSlotGroup.ANY;
            default -> throw new IllegalArgumentException("Slot inconnu : '" + name + "'");
        };
    }
}