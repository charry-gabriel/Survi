package fr.miuby.survi;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Valide l'intégralité de {@code config.yml} sans runtime Bukkit.
 *
 * <h3>Sections couvertes</h3>
 * <ul>
 *   <li>{@code time} — heures de lever/coucher du soleil et de reset quotidien (0–23).</li>
 *   <li>{@code combat} — modificateurs dégâts/résistance {@code > 0}, diviseur de vie {@code ≥ 1}.</li>
 *   <li>{@code reputation.ranks} — liste triée par threshold croissant depuis 0, IDs uniques.</li>
 *   <li>{@code jobs.levels} — exactement 11 entrées, triées, threshold croissant depuis 0.</li>
 *   <li>{@code world-level} — mob-rarity, mob-difficulty cohérents.</li>
 * </ul>
 */
class ConfigTest {

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadConfig() throws IOException {
        Yaml yaml = new Yaml(new LoaderOptions());
        try (InputStream is = Files.newInputStream(Paths.get("src/main/resources/config.yml"))) {
            Map<String, Object> config = (Map<String, Object>) yaml.load(is);
            assertNotNull(config, "config.yml ne doit pas être vide");
            return config;
        }
    }

    // ─── time ─────────────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void timeSectionIsValid() throws IOException {
        Map<String, Object> config = loadConfig();
        Map<String, Object> time = (Map<String, Object>) config.get("time");
        assertNotNull(time, "La section 'time' doit être présente");
        assertHour(time, "sunrise-hour");
        assertHour(time, "sunset-hour");
        assertHour(time, "daily-reset-hour");
    }

    private void assertHour(Map<String, Object> section, String key) {
        assertTrue(section.containsKey(key), "time." + key + " doit être présent");
        assertInstanceOf(Integer.class, section.get(key), "time." + key + " doit être un entier");
        int h = (Integer) section.get(key);
        assertTrue(h >= 0 && h <= 23, "time." + key + " doit être entre 0 et 23, trouvé : " + h);
    }

    // ─── combat ───────────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void combatSectionIsValid() throws IOException {
        Map<String, Object> config = loadConfig();
        Map<String, Object> combat = (Map<String, Object>) config.get("combat");
        assertNotNull(combat, "La section 'combat' doit être présente");

        Map<String, Object> normal = (Map<String, Object>) combat.get("normal");
        assertNotNull(normal, "combat.normal doit être présent");
        assertPositiveNumber(normal, "damage-modifier", "combat.normal");
        assertPositiveNumber(normal, "resistance-modifier", "combat.normal");

        Map<String, Object> end = (Map<String, Object>) combat.get("end");
        assertNotNull(end, "combat.end doit être présent");
        assertPositiveNumber(end, "damage-modifier", "combat.end");
        assertPositiveNumber(end, "resistance-modifier", "combat.end");

        assertTrue(combat.containsKey("death-life-divisor"), "combat.death-life-divisor doit être présent");
        assertInstanceOf(Integer.class, combat.get("death-life-divisor"), "combat.death-life-divisor doit être un entier");
        assertTrue((Integer) combat.get("death-life-divisor") >= 1, "combat.death-life-divisor doit être ≥ 1");
    }

    private void assertPositiveNumber(Map<String, Object> section, String key, String ctx) {
        assertTrue(section.containsKey(key), ctx + "." + key + " doit être présent");
        assertInstanceOf(Number.class, section.get(key), ctx + "." + key + " doit être un nombre");
        assertTrue(((Number) section.get(key)).doubleValue() > 0, ctx + "." + key + " doit être > 0, trouvé : " + section.get(key));
    }

    // ─── reputation ───────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void reputationSectionIsValid() throws IOException {
        Map<String, Object> config = loadConfig();
        Map<String, Object> reputation = (Map<String, Object>) config.get("reputation");
        assertNotNull(reputation, "La section 'reputation' doit être présente");

        List<?> ranks = (List<?>) reputation.get("ranks");
        assertNotNull(ranks, "reputation.ranks doit être présent");
        assertFalse(ranks.isEmpty(), "reputation.ranks ne doit pas être vide");

        int previousThreshold = -1;
        Set<String> seenIds = new HashSet<>();
        for (int i = 0; i < ranks.size(); i++) {
            Map<String, Object> rank = (Map<String, Object>) ranks.get(i);
            String ctx = "reputation.ranks[" + i + "]";
            assertNotNull(rank, ctx + " ne doit pas être null");

            assertTrue(rank.containsKey("id"), ctx + " : champ 'id' manquant");
            String id = String.valueOf(rank.get("id"));
            assertFalse(id.isBlank(), ctx + " : 'id' ne doit pas être vide");
            assertTrue(seenIds.add(id), ctx + " : id '" + id + "' en double");

            assertTrue(rank.containsKey("threshold"), ctx + " : champ 'threshold' manquant");
            assertInstanceOf(Integer.class, rank.get("threshold"), ctx + " : 'threshold' doit être un entier");
            int threshold = (Integer) rank.get("threshold");
            assertTrue(threshold >= 0, ctx + " : 'threshold' doit être ≥ 0, trouvé : " + threshold);
            assertTrue(threshold > previousThreshold, ctx + " : rangs non triés — précédent : " + previousThreshold + ", actuel : " + threshold);
            previousThreshold = threshold;

            assertTrue(rank.containsKey("display"), ctx + " : champ 'display' manquant");
            assertFalse(String.valueOf(rank.get("display")).isBlank(), ctx + " : 'display' ne doit pas être vide");
        }

        assertEquals(0, ((Map<?, ?>) ranks.get(0)).get("threshold"), "Le premier rang doit avoir threshold = 0");
    }

    // ─── jobs ─────────────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void jobsSectionIsValid() throws IOException {
        Map<String, Object> config = loadConfig();
        Map<String, Object> jobs = (Map<String, Object>) config.get("jobs");
        assertNotNull(jobs, "La section 'jobs' doit être présente");

        List<?> levels = (List<?>) jobs.get("levels");
        assertNotNull(levels, "jobs.levels doit être présent");
        assertEquals(11, levels.size(), "jobs.levels doit contenir exactement 11 entrées (niveaux 0–10), trouvé : " + levels.size());

        int previousThreshold = -1;
        for (int i = 0; i < levels.size(); i++) {
            Map<String, Object> level = (Map<String, Object>) levels.get(i);
            String ctx = "jobs.levels[" + i + "]";
            assertNotNull(level, ctx + " ne doit pas être null");

            assertTrue(level.containsKey("threshold"), ctx + " : champ 'threshold' manquant");
            assertInstanceOf(Integer.class, level.get("threshold"), ctx + " : 'threshold' doit être un entier");
            int threshold = (Integer) level.get("threshold");
            assertTrue(threshold >= 0, ctx + " : 'threshold' doit être ≥ 0, trouvé : " + threshold);
            assertTrue(threshold > previousThreshold, ctx + " : niveaux non triés — précédent : " + previousThreshold + ", actuel : " + threshold);
            previousThreshold = threshold;
        }

        assertEquals(0, ((Map<?, ?>) levels.get(0)).get("threshold"), "Le premier niveau de métier doit avoir threshold = 0");
    }

    // ─── world-level ──────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void worldLevelSectionIsValid() throws IOException {
        Map<String, Object> config = loadConfig();
        Map<String, Object> worldLevel = (Map<String, Object>) config.get("world-level");
        assertNotNull(worldLevel, "La section 'world-level' doit être présente");

        // mob-rarity
        Map<String, Object> mobRarity = (Map<String, Object>) worldLevel.get("mob-rarity");
        assertNotNull(mobRarity, "world-level.mob-rarity doit être présent");
        assertNonNegativeNumber(mobRarity, "base", "world-level.mob-rarity");
        assertNonNegativeNumber(mobRarity, "per-level", "world-level.mob-rarity");
        assertNonNegativeNumber(mobRarity, "cap", "world-level.mob-rarity");
        double rarityBase = ((Number) mobRarity.get("base")).doubleValue();
        double rarityCap  = ((Number) mobRarity.get("cap")).doubleValue();
        assertTrue(rarityCap >= rarityBase, "world-level.mob-rarity.cap (" + rarityCap + ") doit être ≥ base (" + rarityBase + ")");

        // mob-difficulty
        Map<String, Object> mobDifficulty = (Map<String, Object>) worldLevel.get("mob-difficulty");
        assertNotNull(mobDifficulty, "world-level.mob-difficulty doit être présent");
        assertNonNegativeNumber(mobDifficulty, "base", "world-level.mob-difficulty");
        assertNonNegativeNumber(mobDifficulty, "per-level", "world-level.mob-difficulty");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────────

    private void assertNonNegativeNumber(Map<String, Object> section, String key, String ctx) {
        assertTrue(section.containsKey(key), ctx + "." + key + " doit être présent");
        assertInstanceOf(Number.class, section.get(key), ctx + "." + key + " doit être un nombre");
        assertTrue(((Number) section.get(key)).doubleValue() >= 0, ctx + "." + key + " doit être ≥ 0");
    }

    private void assertNonNegativeInt(Map<String, Object> section, String key, String ctx) {
        assertTrue(section.containsKey(key), ctx + "." + key + " doit être présent");
        assertInstanceOf(Integer.class, section.get(key), ctx + "." + key + " doit être un entier");
        assertTrue((Integer) section.get(key) >= 0, ctx + "." + key + " doit être ≥ 0");
    }

    private void assertIntInRange(Map<String, Object> section, String key, String ctx, int min, int max) {
        assertTrue(section.containsKey(key), ctx + "." + key + " doit être présent");
        assertInstanceOf(Integer.class, section.get(key), ctx + "." + key + " doit être un entier");
        int val = (Integer) section.get(key);
        assertTrue(val >= min && val <= max, ctx + "." + key + " doit être entre " + min + " et " + max + ", trouvé : " + val);
    }

    private void assertIntField(Map<String, Object> section, String key, String ctx) {
        assertTrue(section.containsKey(key), ctx + "." + key + " doit être présent");
        assertInstanceOf(Integer.class, section.get(key), ctx + "." + key + " doit être un entier");
    }
}