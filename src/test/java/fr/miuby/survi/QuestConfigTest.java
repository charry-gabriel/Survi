package fr.miuby.survi;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Valide la structure des fichiers {@code src/main/resources/quests/*.yml} sans runtime Bukkit.
 *
 * <h3>Ce qui est vérifié</h3>
 * <ul>
 *   <li>Présence du dossier {@code quests/} avec au moins un fichier {@code .yml}.</li>
 *   <li>Chaque fichier possède la clé racine {@code quests} avec au moins une entrée.</li>
 *   <li>Pour chaque quête : {@code id}, {@code name}, {@code description}, {@code type},
 *       {@code difficulty}, {@code targets}, {@code goal} et {@code rewards} requis.</li>
 *   <li>Unicité des {@code id} sur l'ensemble des fichiers du dossier.</li>
 *   <li>{@code type} ∈ {MINE, KILL, BREED, FISH, SHEAR, CRAFT, SMELT}.</li>
 *   <li>{@code difficulty} : entier ≥ 1.</li>
 *   <li>{@code goal} : entier ≥ 1.</li>
 *   <li>{@code targets} : null ou liste non vide de strings non vides.</li>
 *   <li>{@code jobs} : si présent, liste non vide de noms EJob valides.</li>
 *   <li>{@code rewards} : non vide — chaque récompense validée par type
 *       (REPUTATION, POTION, MAX_HEALTH, RESISTANCE, DAMAGE, DISPEL, UNLOCK_TOOL,
 *        UNLOCK_ARMOR, LOCK_WORLD, LIMIT_WORLD, MESSAGE, WORLD_LEVEL, WORLD_RESET).</li>
 * </ul>
 */
class QuestConfigTest {

    private static final Set<String> VALID_QUEST_TYPES = Set.of(
            "MINE", "KILL", "BREED", "FISH", "SHEAR", "CRAFT", "SMELT"
    );

    private static final Set<String> VALID_REWARD_TYPES = Set.of(
            "REPUTATION", "POTION", "MAX_HEALTH", "RESISTANCE", "DAMAGE",
            "DISPEL", "UNLOCK_TOOL", "UNLOCK_ARMOR", "LOCK_WORLD", "LIMIT_WORLD",
            "MESSAGE", "WORLD_LEVEL", "WORLD_RESET"
    );

    /** Lit les jobs valides depuis le schéma JSON (source de vérité = EJob). */
    @SuppressWarnings("unchecked")
    private static Set<String> validJobNames() {
        try {
            String json = Files.readString(Paths.get("src/main/resources/schema/quests-schema.json"));
            Yaml yaml = new Yaml(new LoaderOptions());
            Map<String, Object> schema = (Map<String, Object>) yaml.load(json);
            Map<String, Object> props  = (Map<String, Object>) schema.get("properties");
            Map<String, Object> quests = (Map<String, Object>) props.get("quests");
            Map<String, Object> items  = (Map<String, Object>) quests.get("items");
            Map<String, Object> qProps = (Map<String, Object>) items.get("properties");
            Map<String, Object> jobs   = (Map<String, Object>) qProps.get("jobs");
            Map<String, Object> jItems = (Map<String, Object>) jobs.get("items");
            List<String> values = (List<String>) jItems.get("enum");
            if (values != null && !values.isEmpty()) return new java.util.LinkedHashSet<>(values);
        } catch (IOException | ClassCastException | NullPointerException ignored) {}
        // Fallback statique
        return Set.of("MINEUR", "BUCHERON", "FERMIER", "COMBATANT", "ALCHIMISTE",
                "ENCHANTEUR", "FORGERON", "PECHEUR", "CHASSEUR", "MARCHAND",
                "AVENTURIER", "BATISSEUR");
    }

    // ─── Tests ────────────────────────────────────────────────────────────────────

    @Test
    void testFolderExists() {
        File folder = new File("src/main/resources/quests");
        assertTrue(folder.isDirectory(), "Dossier quests/ introuvable dans src/main/resources/");
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        assertNotNull(files, "Impossible de lister src/main/resources/quests/");
        assertTrue(files.length > 0, "Aucun fichier .yml dans src/main/resources/quests/");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testEachFileHasRootKey() {
        for (File file : getQuestFiles()) {
            Map<String, Object> root = loadFile(file);
            assertTrue(root.containsKey("quests"),
                    "La clé racine 'quests' est absente dans " + file.getName());
            Object raw = root.get("quests");
            assertNotNull(raw, "La section 'quests' est nulle dans " + file.getName());
            assertInstanceOf(List.class, raw, "La section 'quests' doit être une liste dans " + file.getName());
            assertFalse(((List<?>) raw).isEmpty(), file.getName() + " doit contenir au moins une quête");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUniqueIds() {
        List<?> quests = getAllQuests();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < quests.size(); i++) {
            Map<String, Object> quest = (Map<String, Object>) quests.get(i);
            String id = String.valueOf(quest.get("id"));
            assertFalse(seen.contains(id), "ID en double (global) : '" + id + "' à l'index " + i);
            seen.add(id);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testValidStructure() {
        Set<String> validJobs = validJobNames();
        List<?> quests = getAllQuests();

        for (int i = 0; i < quests.size(); i++) {
            Map<String, Object> quest = (Map<String, Object>) quests.get(i);
            String ctx = "quests[" + i + "] (id=" + quest.get("id") + ")";

            // Champs string obligatoires
            assertStringField(quest, "id", ctx);
            assertStringField(quest, "name", ctx);
            assertStringField(quest, "description", ctx);

            // type
            assertTrue(quest.containsKey("type"), ctx + " : champ 'type' manquant");
            String type = String.valueOf(quest.get("type")).toUpperCase();
            assertTrue(VALID_QUEST_TYPES.contains(type),
                    ctx + " : type inconnu '" + type + "'. Valides : " + VALID_QUEST_TYPES);

            // difficulty
            assertTrue(quest.containsKey("difficulty"), ctx + " : champ 'difficulty' manquant");
            assertInstanceOf(Integer.class, quest.get("difficulty"), ctx + " : 'difficulty' doit être un entier");
            assertTrue((Integer) quest.get("difficulty") >= 1, ctx + " : 'difficulty' doit être ≥ 1");

            // targets (null autorisé pour FISH, sinon liste non vide de strings)
            assertTrue(quest.containsKey("targets"),
                    ctx + " : champ 'targets' manquant (utiliser 'null' explicitement si non applicable)");
            Object targetsRaw = quest.get("targets");
            if (targetsRaw != null) {
                assertInstanceOf(List.class, targetsRaw, ctx + " : 'targets' doit être une liste ou null");
                List<?> targetList = (List<?>) targetsRaw;
                assertFalse(targetList.isEmpty(), ctx + " : 'targets' ne doit pas être une liste vide (utiliser null à la place)");
                for (int t = 0; t < targetList.size(); t++) {
                    Object tRaw = targetList.get(t);
                    assertNotNull(tRaw, ctx + ".targets[" + t + "] est null");
                    assertFalse(String.valueOf(tRaw).isBlank(), ctx + ".targets[" + t + "] ne doit pas être vide");
                }
            }

            // goal
            assertTrue(quest.containsKey("goal"), ctx + " : champ 'goal' manquant");
            assertInstanceOf(Integer.class, quest.get("goal"), ctx + " : 'goal' doit être un entier");
            assertTrue((Integer) quest.get("goal") >= 1, ctx + " : 'goal' doit être ≥ 1");

            // jobs (optionnel, mais si présent doit être une liste de jobs valides)
            if (quest.containsKey("jobs") && quest.get("jobs") != null) {
                assertInstanceOf(List.class, quest.get("jobs"), ctx + " : 'jobs' doit être une liste");
                List<?> jobs = (List<?>) quest.get("jobs");
                for (int j = 0; j < jobs.size(); j++) {
                    String job = String.valueOf(jobs.get(j)).toUpperCase();
                    assertTrue(validJobs.contains(job),
                            ctx + ".jobs[" + j + "] : métier inconnu '" + job + "'. Valides : " + validJobs);
                }
            }

            // rewards
            assertTrue(quest.containsKey("rewards"), ctx + " : champ 'rewards' manquant");
            Object rewardsRaw = quest.get("rewards");
            assertNotNull(rewardsRaw, ctx + " : 'rewards' est null");
            assertInstanceOf(List.class, rewardsRaw, ctx + " : 'rewards' doit être une liste");
            List<?> rewards = (List<?>) rewardsRaw;
            assertFalse(rewards.isEmpty(), ctx + " : 'rewards' ne doit pas être vide");

            for (int r = 0; r < rewards.size(); r++) {
                assertReward(ctx + ".rewards[" + r + "]", rewards.get(r), validJobs);
            }
        }
    }

    // ─── Validation d'une récompense ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void assertReward(String ctx, Object raw, Set<String> validJobs) {
        assertNotNull(raw, ctx + " est null");
        assertInstanceOf(Map.class, raw, ctx + " doit être un objet YAML");
        Map<String, Object> reward = (Map<String, Object>) raw;

        assertTrue(reward.containsKey("type"), ctx + " : champ 'type' manquant");
        String type = String.valueOf(reward.get("type")).toUpperCase();
        assertTrue(VALID_REWARD_TYPES.contains(type),
                ctx + " : type de récompense inconnu '" + type + "'. Valides : " + VALID_REWARD_TYPES);

        switch (type) {
            case "REPUTATION" -> {
                assertTrue(reward.containsKey("job"), ctx + " (REPUTATION) : champ 'job' manquant");
                String job = String.valueOf(reward.get("job")).toUpperCase();
                assertTrue(validJobs.contains(job), ctx + " (REPUTATION) : job inconnu '" + job + "'");
                assertTrue(reward.containsKey("value"), ctx + " (REPUTATION) : champ 'value' manquant");
                assertInstanceOf(Integer.class, reward.get("value"), ctx + " (REPUTATION) : 'value' doit être un entier");
                assertTrue((Integer) reward.get("value") >= 1, ctx + " (REPUTATION) : 'value' doit être ≥ 1");
            }
            case "POTION" -> {
                assertTrue(reward.containsKey("potion"), ctx + " (POTION) : champ 'potion' manquant");
                assertFalse(String.valueOf(reward.get("potion")).isBlank(), ctx + " (POTION) : 'potion' ne doit pas être vide");
                assertTrue(reward.containsKey("duration"), ctx + " (POTION) : champ 'duration' manquant");
                assertInstanceOf(Integer.class, reward.get("duration"), ctx + " (POTION) : 'duration' doit être un entier");
                assertTrue((Integer) reward.get("duration") >= 1, ctx + " (POTION) : 'duration' doit être ≥ 1");
                assertTrue(reward.containsKey("amplifier"), ctx + " (POTION) : champ 'amplifier' manquant");
                assertInstanceOf(Integer.class, reward.get("amplifier"), ctx + " (POTION) : 'amplifier' doit être un entier");
                assertTrue((Integer) reward.get("amplifier") >= 0, ctx + " (POTION) : 'amplifier' doit être ≥ 0");
            }
            case "MAX_HEALTH" -> {
                assertTrue(reward.containsKey("value"), ctx + " (MAX_HEALTH) : champ 'value' manquant");
                assertInstanceOf(Integer.class, reward.get("value"), ctx + " (MAX_HEALTH) : 'value' doit être un entier");
            }
            case "RESISTANCE", "DAMAGE" -> {
                assertTrue(reward.containsKey("value"), ctx + " (" + type + ") : champ 'value' manquant");
                assertInstanceOf(Number.class, reward.get("value"), ctx + " (" + type + ") : 'value' doit être un nombre");
            }
            case "DISPEL" -> {
                assertTrue(reward.containsKey("value"), ctx + " (DISPEL) : champ 'value' manquant");
                assertInstanceOf(Integer.class, reward.get("value"), ctx + " (DISPEL) : 'value' doit être un entier");
                assertTrue((Integer) reward.get("value") >= 1, ctx + " (DISPEL) : 'value' doit être ≥ 1");
            }
            case "UNLOCK_TOOL" -> assertTrue(reward.containsKey("tool"), ctx + " (UNLOCK_TOOL) : champ 'tool' manquant");
            case "UNLOCK_ARMOR" -> assertTrue(reward.containsKey("armor"), ctx + " (UNLOCK_ARMOR) : champ 'armor' manquant");
            case "LOCK_WORLD", "LIMIT_WORLD" -> assertTrue(reward.containsKey("world"), ctx + " (" + type + ") : champ 'world' manquant");
            case "MESSAGE" -> {
                assertTrue(reward.containsKey("text"), ctx + " (MESSAGE) : champ 'text' manquant");
                assertFalse(String.valueOf(reward.get("text")).isBlank(), ctx + " (MESSAGE) : 'text' ne doit pas être vide");
            }
            case "WORLD_LEVEL" -> {
                assertTrue(reward.containsKey("levels"), ctx + " (WORLD_LEVEL) : champ 'levels' manquant");
                assertInstanceOf(Integer.class, reward.get("levels"), ctx + " (WORLD_LEVEL) : 'levels' doit être un entier");
                assertTrue((Integer) reward.get("levels") >= 1, ctx + " (WORLD_LEVEL) : 'levels' doit être ≥ 1");
            }
            case "WORLD_RESET" -> {
                assertTrue(reward.containsKey("frequency"), ctx + " (WORLD_RESET) : champ 'frequency' manquant");
                assertInstanceOf(Integer.class, reward.get("frequency"), ctx + " (WORLD_RESET) : 'frequency' doit être un entier");
                assertTrue((Integer) reward.get("frequency") >= 0, ctx + " (WORLD_RESET) : 'frequency' doit être ≥ 0");
            }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────────

    private void assertStringField(Map<String, Object> map, String key, String ctx) {
        assertTrue(map.containsKey(key), ctx + " : champ '" + key + "' manquant");
        assertNotNull(map.get(key), ctx + " : '" + key + "' est null");
        assertFalse(String.valueOf(map.get(key)).isBlank(), ctx + " : '" + key + "' ne doit pas être vide");
    }

    /** Retourne les fichiers .yml du dossier quests/, triés alphabétiquement. */
    private File[] getQuestFiles() {
        File folder = new File("src/main/resources/quests");
        assertTrue(folder.isDirectory(), "Dossier quests/ introuvable");
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        assertNotNull(files, "Impossible de lister src/main/resources/quests/");
        assertTrue(files.length > 0, "Aucun fichier .yml dans quests/");
        Arrays.sort(files);
        return files;
    }

    /** Charge un seul fichier YAML et retourne sa racine. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadFile(File file) {
        try (InputStream in = new FileInputStream(file)) {
            Yaml yaml = new Yaml(new LoaderOptions());
            Object loaded = yaml.load(in);
            assertNotNull(loaded, file.getName() + " est vide ou n'a pas pu être parsé");
            assertInstanceOf(Map.class, loaded, "La racine de " + file.getName() + " doit être un objet YAML");
            return (Map<String, Object>) loaded;
        } catch (Exception e) {
            fail("Erreur de parsing YAML dans " + file.getName() + " : " + e.getMessage());
            throw new RuntimeException(e); // unreachable
        }
    }

    /** Fusionne toutes les listes {@code quests} de l'ensemble des fichiers du dossier. */
    @SuppressWarnings("unchecked")
    private List<?> getAllQuests() {
        List<Object> all = new ArrayList<>();
        for (File file : getQuestFiles()) {
            Map<String, Object> root = loadFile(file);
            assertTrue(root.containsKey("quests"), "Section 'quests' absente dans " + file.getName());
            Object raw = root.get("quests");
            assertInstanceOf(List.class, raw, "Section 'quests' invalide dans " + file.getName());
            all.addAll((List<Object>) raw);
        }
        return all;
    }
}