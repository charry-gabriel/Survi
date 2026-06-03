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
import java.util.Objects;
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
            List<Object> targets,
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
     * liste de BlessingEffects :</p>
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

        List<Object> targets = parseTargets(map, type);

        List<Map<String, Object>> rawRewards = (List<Map<String, Object>>) map.get("rewards");
        Blessing rewards = BlessingLoader.loadFromList(id, rawRewards);
        if (rewards == null) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.QUEST,
                    "[QuestYamlLoader] Aucune récompense définie pour la quête '" + id + "'");
            rewards = new Blessing(new BlessingEffect[0]);
        }

        return new BaseFields(id, name, description, type, targets, goal, rewards);
    }

    /**
     * Parse la liste de cibles selon le type de quête.
     *
     * <p>Formats YAML acceptés :</p>
     * <ul>
     *   <li>{@code targets: null} — aucune cible spécifique (toute cible acceptée)</li>
     *   <li>{@code targets: [IRON_ORE]} — cible unique</li>
     *   <li>{@code targets: [IRON_ORE, DEEPSLATE_IRON_ORE]} — cibles multiples</li>
     * </ul>
     *
     * @return liste des objets Material/EntityType correspondants, ou {@code null} pour FISH ou si {@code targets} est absent/null
     */
    @SuppressWarnings("unchecked")
    public static List<Object> parseTargets(Map<String, Object> map, EQuestType type) {
        Object raw = map.get("targets");
        if (raw == null) return null;

        List<String> targetStrings;
        if (raw instanceof List<?> list) {
            targetStrings = (List<String>) list;
        } else if (raw instanceof String s) {
            // Tolérance : accepte une chaîne simple en plus du format liste
            targetStrings = List.of(s);
        } else {
            return null;
        }

        List<Object> parsed = targetStrings.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> (Object) switch (type) {
                    case MINE, CRAFT, SMELT -> Material.valueOf(s);
                    case KILL, SHEAR, BREED -> EntityType.valueOf(s);
                    default -> null;
                })
                .filter(Objects::nonNull)
                .toList();

        return parsed.isEmpty() ? null : parsed;
    }
}