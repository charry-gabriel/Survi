package fr.miuby.survi.quest;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.blessing.Blessing;
import fr.miuby.survi.blessing.BlessingEffect;
import fr.miuby.survi.blessing.BlessingLoader;
import fr.miuby.survi.system.log.ELogTag;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

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
            Blessing rewards
    ) {}

    // -------------------------------------------------------------------------
    // API
    // -------------------------------------------------------------------------

    /**
     * Parse les champs communs d'un bloc quête.
     *
     * <p>Les récompenses sont définies sous la clé {@code rewards} comme une
     * liste de BlessingEffects :
     * <pre>
     * rewards:
     *   - type: REPUTATION
     *     job: MINEUR
     *     value: 50
     *   - type: POTION
     *     potion: haste
     *     duration: 1728000
     *     amplifier: 0
     * </pre>
     *
     * @throws Exception si un champ obligatoire est absent ou invalide
     */
    @SuppressWarnings("unchecked")
    public static BaseFields parseBase(Map<String, Object> map) {
        String id          = (String) map.get("id");
        String name        = (String) map.get("name");
        String description = (String) map.get("description");
        EQuestType type    = EQuestType.valueOf((String) map.get("type"));
        int goal           = ((Number) map.get("goal")).intValue();

        Object target = parseTarget(map, type);

        List<Map<String, Object>> rawRewards = (List<Map<String, Object>>) map.get("rewards");
        Blessing rewards = BlessingLoader.loadFromList(id, rawRewards);
        if (rewards == null) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.QUEST,
                    "[QuestYamlLoader] Aucune récompense définie pour la quête '" + id + "'");
            rewards = new Blessing(new BlessingEffect[0]);
        }

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
}
