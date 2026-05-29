package fr.miuby.survi.quest;

import fr.miuby.survi.system.log.LogManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Utilitaire de parsing YAML partagé entre {@link QuestManager}
 * et {@link GlobalQuestManager}.
 */
public final class QuestYamlLoader {

    private QuestYamlLoader() {}

    // -------------------------------------------------------------------------
    // Record de retour pour les champs communs
    // -------------------------------------------------------------------------

    /**
     * Champs communs aux deux types de quêtes, tels que parsés depuis un
     * bloc YAML (Map issue de {@code config.getList(...)}).
     */
    public record BaseFields(
            String id,
            String name,
            String description,
            EQuestType type,
            Object target,
            int goal,
            List<PotionEffect> potionRewards
    ) {}

    // -------------------------------------------------------------------------
    // API
    // -------------------------------------------------------------------------

    /**
     * Parse les champs communs d'un bloc quête.
     *
     * @throws Exception si un champ obligatoire est absent ou invalide
     */
    public static BaseFields parseBase(Map<String, Object> map) {
        String id          = (String) map.get("id");
        String name        = (String) map.get("name");
        String description = (String) map.get("description");
        EQuestType type    = EQuestType.valueOf((String) map.get("type"));
        int goal           = ((Number) map.get("goal")).intValue();

        Object target          = parseTarget(map, type);
        List<PotionEffect> rewards = parsePotionRewards(map);

        return new BaseFields(id, name, description, type, target, goal, rewards);
    }

    /**
     * Parse la cible selon le type de quête.
     * Retourne {@code null} pour FISH ou si la clé {@code target} est absente.
     */
    public static Object parseTarget(Map<String, Object> map, EQuestType type) {
        String targetStr = (String) map.get("target");
        if (targetStr == null) return null;

        return switch (type) {
            case MINE, CRAFT, SMELT -> Material.valueOf(targetStr);
            case KILL, SHEAR, BREED -> EntityType.valueOf(targetStr);
            default -> null;
        };
    }

    /**
     * Parse les récompenses en effets de potion.
     * Accepte les deux clés YAML utilisées dans le projet :
     * <ul>
     *   <li>{@code rewards} — quêtes journalières (quests.yml)</li>
     *   <li>{@code potion_rewards} — quêtes globales (global_quests.yml)</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    public static List<PotionEffect> parsePotionRewards(Map<String, Object> map) {
        List<PotionEffect> result = new ArrayList<>();

        List<?> raw = (List<?>) map.get("rewards");
        if (raw == null) raw = (List<?>) map.get("potion_rewards");
        if (raw == null) return result;

        for (Object obj : raw) {
            if (!(obj instanceof Map<?, ?> rawEntry)) continue;
            Map<String, Object> entry = (Map<String, Object>) rawEntry;
            try {
                String typeStr = ((String) entry.get("type")).toLowerCase();
                NamespacedKey key = NamespacedKey.minecraft(typeStr);
                PotionEffectType effectType = Registry.POTION_EFFECT_TYPE.get(key);
                if (effectType == null) {
                    LogManager.getInstance().log(Level.WARNING, LogManager.ETagLog.QUEST,
                            "Type de potion inconnu : " + typeStr);
                    continue;
                }
                int duration  = ((Number) entry.get("duration")).intValue();
                int amplifier = ((Number) entry.get("amplifier")).intValue();
                result.add(new PotionEffect(effectType, duration, amplifier));
            } catch (Exception e) {
                LogManager.getInstance().log(Level.WARNING, LogManager.ETagLog.QUEST,
                        "Erreur lors du parsing d'une récompense de potion", e);
            }
        }
        return result;
    }
}
