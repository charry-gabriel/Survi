package fr.miuby.survi.blessing;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.lib.utils.Rect;
import fr.miuby.survi.item.ECustomItem;
import fr.miuby.survi.item.locked_item.ELockedArmorType;
import fr.miuby.survi.item.locked_item.ELockedToolType;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.world.EWorld;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Charge la liste de {@link Blessing} depuis la section {@code blessings} d'un
 * fichier villager YAML, ou depuis une liste de maps pour les quêtes.
 *
 * <p>Types d'effets supportés : ACID_RAIN, MAX_HEALTH, RESISTANCE, DAMAGE, DISPEL,
 * UNLOCK_TOOL, UNLOCK_ARMOR, LOCK_WORLD, MESSAGE, WORLD_LEVEL,
 * WORLD_RESET, LIMIT_WORLD, REPUTATION, POTION, ITEM, FLY.
 */
public class BlessingLoader {

    /**
     * Charge un unique {@link Blessing} depuis une liste d'effets bruts (format POJO SnakeYAML).
     *
     * <p>Format attendu dans le YAML :
     * <pre>
     * rewards:
     *   - type: REPUTATION
     *     job: MINER
     *     value: 50
     *   - type: POTION
     *     potion: haste
     *     duration: 1728000
     *     amplifier: 0
     * </pre>
     *
     * @param contextId identifiant du contexte (villageois, quête…) pour les messages de log
     * @param rawEffects liste de maps représentant les effets (peut être null)
     * @return un {@link Blessing} prêt à l'emploi, ou {@code null} si la liste est vide/null
     */
    public static Blessing loadFromList(String contextId, java.util.List<java.util.Map<String, Object>> rawEffects) {
        if (rawEffects == null || rawEffects.isEmpty()) return null;
        List<BlessingEffect> effects = new ArrayList<>();
        for (java.util.Map<String, Object> map : rawEffects) {
            BlessingEffect effect = parseEffect(contextId, map);
            if (effect != null) effects.add(effect);
        }
        if (effects.isEmpty()) return null;
        return new Blessing(effects.toArray(BlessingEffect[]::new));
    }

    /**
     * Charge les blessings depuis une section YAML {@code blessings}.
     *
     * @param villagerId identifiant du villageois (pour les messages de log)
     * @param blessingsSection la section YAML {@code blessings} (peut être null)
     * @return tableau de {@link Blessing} prêt à l'emploi, jamais null
     */
    public static Blessing[] load(String villagerId, ConfigurationSection blessingsSection) {
        if (blessingsSection == null) return new Blessing[0];

        List<Blessing> blessings = new ArrayList<>();

        for (String key : blessingsSection.getKeys(false)) {
            ConfigurationSection blessingSection = blessingsSection.getConfigurationSection(key);
            if (blessingSection == null) continue;

            List<?> rawEffects = blessingSection.getList("effects");
            if (rawEffects == null) continue;

            List<BlessingEffect> effects = new ArrayList<>();
            for (Object obj : rawEffects) {
                if (!(obj instanceof Map<?, ?> rawMap)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) rawMap;
                BlessingEffect effect = parseEffect(villagerId, map);
                if (effect != null) effects.add(effect);
            }
            blessings.add(new Blessing(effects.toArray(BlessingEffect[]::new)));
        }

        return blessings.toArray(Blessing[]::new);
    }

    private static BlessingEffect parseEffect(String contextId, Map<String, Object> map) {
        String type = String.valueOf(map.get("type")).toUpperCase();
        try {
            return switch (type) {
                case "ACID_RAIN"    -> new AcidRainEffect();
                case "MAX_HEALTH"   -> new MaxHealthEffect(toInt(map.get("value"), 0));
                case "RESISTANCE"   -> new ResistanceEffect(toFloat(map.get("value"), 1f));
                case "DAMAGE"       -> new DamageEffect(toFloat(map.get("value"), 1f));
                case "DISPEL"       -> new DispelEffect(toInt(map.get("value"), 1));
                case "UNLOCK_TOOL"  -> new UnlockToolEffect(ELockedToolType.valueOf(String.valueOf(map.get("tool")).toUpperCase()));
                case "UNLOCK_ARMOR" -> new UnlockArmorEffect(ELockedArmorType.valueOf(String.valueOf(map.get("armor")).toUpperCase()));
                case "LOCK_WORLD"   -> new LockWorldEffect(EWorld.valueOf(String.valueOf(map.get("world")).toUpperCase()));
                case "MESSAGE"      -> new MessageEffect(String.valueOf(map.get("text")));
                case "WORLD_LEVEL"  -> new WorldLevelEffect();
                case "WORLD_RESET"  -> new WorldResetEffect(toInt(map.get("frequency"), 7));
                case "FLY"          -> new FlyEffect();
                case "LIMIT_WORLD"  -> parseLimitWorld(map);
                case "REPUTATION"   -> parseReputation(map);
                case "POTION"       -> parsePotion(contextId, map);
                default -> {
                    MLLogManager.getInstance().log(Level.WARNING, ELogTag.VILLAGER,
                            "[BlessingLoader] Type d'effet inconnu '" + type + "' pour " + contextId);
                    yield null;
                }
            };
        } catch (Exception e) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.VILLAGER,
                    "[BlessingLoader] Erreur parsing effet '" + type + "' pour " + contextId, e);
            return null;
        }
    }

    private static ReputationEffect parseReputation(Map<String, Object> map) {
        EJob job = EJob.valueOf(String.valueOf(map.get("job")).toUpperCase());
        int value = toInt(map.get("value"), 0);
        return new ReputationEffect(job, value);
    }

    private static PotionsEffect parsePotion(String contextId, Map<String, Object> map) {
        String potionType = String.valueOf(map.get("potion")).toLowerCase();
        NamespacedKey key = NamespacedKey.minecraft(potionType);
        PotionEffectType effectType = Registry.POTION_EFFECT_TYPE.get(key);
        if (effectType == null) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.VILLAGER,
                    "[BlessingLoader] Type de potion inconnu : " + potionType + " pour " + contextId);
            return null;
        }
        int duration  = toInt(map.get("duration"), 600);
        int amplifier = toInt(map.get("amplifier"), 0);
        return new PotionsEffect(new PotionEffect(effectType, duration, amplifier));
    }

    /** Résout un identifiant d'item : ECustomItem en priorité (pour récompenser la variante personnalisée plutôt que le vanilla équivalent), fallback sur Material vanilla. */
    private static ItemStack resolveItem(String itemId) {
        ECustomItem custom = ECustomItem.fromString(itemId);
        if (custom != null) return custom.getItemStack();
        try {
            return new ItemStack(Material.valueOf(itemId.toUpperCase()));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static LimitWorldEffect parseLimitWorld(Map<String, Object> map) {
        EWorld world = EWorld.valueOf(String.valueOf(map.get("world")).toUpperCase());
        int minX = toInt(map.get("min-x"), Integer.MIN_VALUE);
        int maxX = toInt(map.get("max-x"), Integer.MAX_VALUE);
        int minY = toInt(map.get("min-y"), Integer.MIN_VALUE);
        int maxY = toInt(map.get("max-y"), Integer.MAX_VALUE);
        int minZ = toInt(map.get("min-z"), Integer.MIN_VALUE);
        int maxZ = toInt(map.get("max-z"), Integer.MAX_VALUE);
        return new LimitWorldEffect(world, new Rect(maxX, minX, maxY, minY, maxZ, minZ));
    }

    private static int   toInt  (Object v, int   def) { return v instanceof Number n ? n.intValue()   : def; }
    private static float toFloat(Object v, float def) { return v instanceof Number n ? n.floatValue() : def; }
}