package fr.miuby.survi.villager.villagerlevel.blessing;

import fr.miuby.lib.utils.Rect;
import fr.miuby.survi.item.locked_item.ELockedArmorType;
import fr.miuby.survi.item.locked_item.ELockedToolType;
import fr.miuby.survi.system.log.LogManager;
import fr.miuby.survi.world.EWorld;
import org.bukkit.configuration.ConfigurationSection;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Charge la liste de {@link Blessing} depuis la section {@code blessings} d'un
 * fichier villager YAML.
 *
 * <p>Format attendu dans le YAML pour chaque palier :
 * <pre>
 * blessings:
 *   - effects:
 *       - type: MAX_HEALTH
 *         value: -20
 *   - effects:
 *       - type: MAX_HEALTH
 *         value: -18
 *       - type: UNLOCK_TOOL
 *         tool: WOOD
 * </pre>
 *
 * <p>Types d'effets supportés : MAX_HEALTH, RESISTANCE, DAMAGE, DISPEL,
 * UNLOCK_TOOL, UNLOCK_ARMOR, LOCK_WORLD, LOCK_VILLAGER, MESSAGE, WORLD_LEVEL,
 * WORLD_RESET, LIMIT_WORLD.
 */
public class BlessingLoader {

    /**
     * Charge un unique {@link Blessing} depuis une liste d'effets bruts (format POJO SnakeYAML).
     *
     * <p>Format attendu dans le YAML pour un level :
     * <pre>
     * blessings:
     *   - type: MAX_HEALTH
     *     value: -20
     *   - type: UNLOCK_TOOL
     *     tool: WOOD
     * </pre>
     *
     * @param villagerId identifiant du villageois (pour les messages de log)
     * @param rawEffects liste de maps représentant les effets (peut être null)
     * @return un {@link Blessing} prêt à l'emploi, ou {@code null} si la liste est vide/null
     */
    public static Blessing loadFromList(String villagerId, java.util.List<java.util.Map<String, Object>> rawEffects) {
        if (rawEffects == null || rawEffects.isEmpty()) return null;
        List<BlessingEffect> effects = new ArrayList<>();
        for (java.util.Map<String, Object> map : rawEffects) {
            BlessingEffect effect = parseEffect(villagerId, map);
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
        List<?> rawList = blessingsSection.getList("");

        // ConfigurationSection.getList("") doesn't work — iterate keys instead
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

    private static BlessingEffect parseEffect(String villagerId, Map<String, Object> map) {
        String type = String.valueOf(map.get("type")).toUpperCase();
        try {
            return switch (type) {
                case "MAX_HEALTH" -> new MaxHealthEffect(toInt(map.get("value"), 0));
                case "RESISTANCE"  -> new ResistanceEffect(toFloat(map.get("value"), 1f));
                case "DAMAGE"      -> new DamageEffect(toFloat(map.get("value"), 1f));
                case "DISPEL"      -> new DispelEffect(toInt(map.get("value"), 1));
                case "UNLOCK_TOOL" -> new UnlockToolEffect(ELockedToolType.valueOf(String.valueOf(map.get("tool")).toUpperCase()));
                case "UNLOCK_ARMOR" -> new UnlockArmorEffect(ELockedArmorType.valueOf(String.valueOf(map.get("armor")).toUpperCase()));
                case "LOCK_WORLD" -> new LockWorldEffect(EWorld.valueOf(String.valueOf(map.get("world")).toUpperCase()));
                case "LOCK_VILLAGER" -> new LockVillagerEffect(Duration.ofDays(toLong(map.get("days"), 1)));
                case "MESSAGE" -> new MessageEffect(String.valueOf(map.get("text")));
                case "WORLD_LEVEL" -> new WorldLevelEffect(toInt(map.get("levels"), 1));
                case "WORLD_RESET" -> new WorldResetEffect(toInt(map.get("frequency"), 7));
                case "LIMIT_WORLD" -> parseLimitWorld(map);
                default -> {
                    LogManager.getInstance().log(Level.WARNING, LogManager.ETagLog.VILLAGER, "[BlessingLoader] Type d'effet inconnu '" + type + "' pour " + villagerId);
                    yield null;
                }
            };
        } catch (Exception e) {
            LogManager.getInstance().log(Level.WARNING, LogManager.ETagLog.VILLAGER, "[BlessingLoader] Erreur parsing effet '" + type + "' pour " + villagerId, e);
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
    private static long  toLong (Object v, long  def) { return v instanceof Number n ? n.longValue()  : def; }
}
