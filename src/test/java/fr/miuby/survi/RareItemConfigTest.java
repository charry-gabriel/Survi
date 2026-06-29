package fr.miuby.survi;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Valide la structure et les plages de valeurs de {@code rare_items.yml}
 * sans runtime Bukkit.
 *
 * <h3>Ce qui est vérifié</h3>
 * <ul>
 *   <li>Présence du fichier YAML et du schéma JSON associé.</li>
 *   <li>Présence et validité des paramètres globaux (max-chance, save-every, min-job-level, suspicious-window-ms).</li>
 *   <li>Présence et validité des 6 entrées de métier.</li>
 *   <li>threshold ≥ 0, growth-range ≥ 1, suspicious-threshold ≥ 1.</li>
 *   <li>Présence et validité de {@code min-distance} pour EXPLORER.</li>
 * </ul>
 */
@SuppressWarnings("unchecked")
class RareItemConfigTest {

    private static final List<String> JOB_NAMES = List.of(
            "MINER", "LUMBERJACK", "FARMER", "ENCHANTER", "FISHERMAN", "EXPLORER");

    private static Map<String, Object> root;

    @BeforeAll
    static void load() throws IOException {
        File file = new File("src/main/resources/rare_items.yml");
        assertTrue(file.exists(), "rare_items.yml introuvable dans src/main/resources/");
        try (FileInputStream fis = new FileInputStream(file)) {
            root = (Map<String, Object>) new Yaml(new LoaderOptions()).load(fis);
            assertNotNull(root, "rare_items.yml est vide ou mal formé");
        }
    }

    // ─── Schéma ──────────────────────────────────────────────────────────────────

    @Test
    void schemaExists() {
        assertTrue(new File("src/main/resources/schema/rare-items-schema.json").exists(),
                "schema/rare-items-schema.json introuvable");
    }

    // ─── Paramètres globaux ───────────────────────────────────────────────────────

    @Test
    void saveEveryValid() {
        Object v = root.get("save-every");
        assertNotNull(v, "save-every absent");
        int val = ((Number) v).intValue();
        assertTrue(val >= 1, "save-every doit être ≥ 1, obtenu : " + val);
    }

    @Test
    void minJobLevelValid() {
        Object v = root.get("min-job-level");
        assertNotNull(v, "min-job-level absent");
        int val = ((Number) v).intValue();
        assertTrue(val >= 0, "min-job-level doit être ≥ 0, obtenu : " + val);
    }

    @Test
    void suspiciousWindowMsValid() {
        Object v = root.get("suspicious-window-ms");
        assertNotNull(v, "suspicious-window-ms absent");
        long val = ((Number) v).longValue();
        assertTrue(val >= 1000, "suspicious-window-ms doit être ≥ 1000, obtenu : " + val);
    }

    // ─── Entrées par métier ───────────────────────────────────────────────────────

    @Test
    void allJobsPresent() {
        Map<String, Object> jobs = (Map<String, Object>) root.get("jobs");
        assertNotNull(jobs, "section 'jobs' absente");
        for (String name : JOB_NAMES) {
            assertTrue(jobs.containsKey(name), "métier absent dans rare_items.yml : " + name);
        }
    }

    @Test
    void jobThresholdsValid() {
        Map<String, Object> jobs = (Map<String, Object>) root.get("jobs");
        for (String name : JOB_NAMES) {
            Map<String, Object> job = (Map<String, Object>) jobs.get(name);
            Object v = job.get("threshold");
            assertNotNull(v, name + ".threshold absent");
            long val = ((Number) v).longValue();
            assertTrue(val >= 0, name + ".threshold doit être ≥ 0, obtenu : " + val);
        }
    }

    @Test
    void jobGrowthRangesValid() {
        Map<String, Object> jobs = (Map<String, Object>) root.get("jobs");
        for (String name : JOB_NAMES) {
            Map<String, Object> job = (Map<String, Object>) jobs.get(name);
            Object v = job.get("growth-range");
            assertNotNull(v, name + ".growth-range absent");
            long val = ((Number) v).longValue();
            assertTrue(val >= 1, name + ".growth-range doit être ≥ 1, obtenu : " + val);
        }
    }

    @Test
    void jobSuspiciousThresholdsValid() {
        Map<String, Object> jobs = (Map<String, Object>) root.get("jobs");
        for (String name : JOB_NAMES) {
            Map<String, Object> job = (Map<String, Object>) jobs.get(name);
            Object v = job.get("suspicious-threshold");
            assertNotNull(v, name + ".suspicious-threshold absent");
            long val = ((Number) v).longValue();
            assertTrue(val >= 1, name + ".suspicious-threshold doit être ≥ 1, obtenu : " + val);
        }
    }

    @Test
    void explorerMinDistanceValid() {
        Map<String, Object> jobs = (Map<String, Object>) root.get("jobs");
        Map<String, Object> explorer = (Map<String, Object>) jobs.get("EXPLORER");
        Object v = explorer.get("min-distance");
        assertNotNull(v, "EXPLORER.min-distance absent");
        double val = ((Number) v).doubleValue();
        assertTrue(val >= 0, "EXPLORER.min-distance doit être ≥ 0, obtenu : " + val);
    }

    @Test
    void jobMaxChanceValid() {
        Map<String, Object> jobs = (Map<String, Object>) root.get("jobs");
        for (String name : JOB_NAMES) {
            Map<String, Object> job = (Map<String, Object>) jobs.get(name);
            Object v = job.get("max-chance");
            assertNotNull(v, name + ".max-chance absent");
            double val = ((Number) v).doubleValue();
            assertTrue(val > 0 && val <= 1.0,
                    name + ".max-chance doit être dans ]0 ; 1], obtenu : " + val);
        }
    }
}