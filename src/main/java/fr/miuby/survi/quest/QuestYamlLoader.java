package fr.miuby.survi.quest;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.blessing.Blessing;
import fr.miuby.survi.blessing.BlessingEffect;
import fr.miuby.survi.blessing.BlessingLoader;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.quest.globalquest.GlobalQuest;
import fr.miuby.survi.quest.quest.Quest;
import fr.miuby.survi.system.log.ELogTag;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * Chargement YAML des quêtes journalières et globales.
 * Seule classe autorisée à toucher les fichiers du dossier {@code quests/} et {@code global_quests.yml}.
 */
public final class QuestYamlLoader {

    private QuestYamlLoader() {}

    // =========================================================================
    // Points d'entrée publics
    // =========================================================================

    /**
     * Charge et retourne toutes les quêtes journalières depuis le dossier {@code quests/}.
     * Chaque fichier {@code .yml} du dossier est lu indépendamment puis fusionné.
     * L'ordre de chargement est alphabétique pour garantir la reproductibilité.
     */
    public static List<Quest> loadQuests() {
        File folder = new File(GameManager.getInstance().getPlugin().getDataFolder(), "quests");
        if (!folder.isDirectory()) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.QUEST, "Dossier 'quests/' introuvable dans le dataFolder");
            return List.of();
        }
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.QUEST, "Aucun fichier .yml trouvé dans 'quests/'");
            return List.of();
        }
        Arrays.sort(files); // ordre alphabétique déterministe
        List<Quest> result = new ArrayList<>();
        for (File file : files) {
            result.addAll(loadFromFile(file, "quests", QuestYamlLoader::buildQuest, "quête(s)"));
        }
        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                result.size() + " quête(s) chargée(s) au total depuis quests/");
        return result;
    }

    /** Charge et retourne toutes les quêtes globales depuis {@code global_quests.yml}. */
    public static List<GlobalQuest> loadGlobalQuests() {
        return loadFromFile("global_quests.yml", "global_quests", QuestYamlLoader::buildGlobalQuest, "quête(s) globale(s)");
    }

    // =========================================================================
    // Chargement générique (fichier → liste de quêtes)
    // =========================================================================

    /**
     * Ouvre un fichier YAML par nom relatif au dataFolder, itère sur la liste
     * à la clé {@code rootKey} et applique {@code builder} sur chaque entrée.
     */
    private static <T extends BaseQuest> List<T> loadFromFile(
            String filename, String rootKey, Function<Map<String, Object>, T> builder, String logLabel) {
        File file = new File(GameManager.getInstance().getPlugin().getDataFolder(), filename);
        return loadFromFile(file, rootKey, builder, logLabel);
    }

    /**
     * Ouvre un fichier YAML, itère sur la liste à la clé {@code rootKey} et applique
     * {@code builder} sur chaque entrée. Les erreurs par entrée sont loggées et ignorées.
     */
    private static <T extends BaseQuest> List<T> loadFromFile(
            File file, String rootKey, Function<Map<String, Object>, T> builder, String logLabel) {

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<T> result = new ArrayList<>();

        if (!config.contains(rootKey)) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.QUEST, "Clé '" + rootKey + "' absente dans " + file.getName());
            return result;
        }

        List<?> rawList = config.getList(rootKey);
        if (rawList == null) return result;

        for (Object obj : rawList) {
            if (!(obj instanceof Map<?, ?> rawMap)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) rawMap;
            try {
                T quest = builder.apply(map);
                if (quest != null) result.add(quest);
            } catch (Exception e) {
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.QUEST,
                        "Erreur lors du chargement d'une entrée dans " + file.getName(), e);
            }
        }

        MLLogManager.getInstance().log(Level.INFO, ELogTag.QUEST,
                result.size() + " " + logLabel + " chargée(s) depuis " + file.getName());
        return result;
    }

    // =========================================================================
    // Builders spécifiques par type de quête
    // =========================================================================

    private static Quest buildQuest(Map<String, Object> map) {
        BaseFields base = parseBase(map);

        int difficulty = ((Number) map.get("difficulty")).intValue();

        @SuppressWarnings("unchecked")
        List<String> rawJobs = (List<String>) map.get("jobs");
        List<EJob> jobs = (rawJobs == null || rawJobs.isEmpty())
                ? List.of()
                : rawJobs.stream().map(j -> EJob.valueOf(j.toUpperCase())).toList();

        return Quest.builder()
                .id(base.id()).name(base.name()).description(base.description())
                .type(base.type()).targets(base.targets()).goal(base.goal()).rewards(base.rewards())
                .difficulty(difficulty).jobs(jobs)
                .build();
    }

    private static GlobalQuest buildGlobalQuest(Map<String, Object> map) {
        BaseFields base = parseBase(map);
        int timeLimit = ((Number) map.get("time_limit")).intValue();

        return GlobalQuest.builder()
                .id(base.id()).name(base.name()).description(base.description())
                .type(base.type()).targets(base.targets()).goal(base.goal()).rewards(base.rewards())
                .timeLimitSeconds(timeLimit)
                .build();
    }

    // =========================================================================
    // Parsing des champs communs (BaseQuest)
    // =========================================================================

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

    @SuppressWarnings("unchecked")
    private static BaseFields parseBase(Map<String, Object> map) {
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
    private static List<Object> parseTargets(Map<String, Object> map, EQuestType type) {
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