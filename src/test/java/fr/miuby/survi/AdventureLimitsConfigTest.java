package fr.miuby.survi;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Valide la section {@code adventure-limits} de {@code config.yml} sans runtime Bukkit.
 *
 * <h3>Ce qui est vérifié</h3>
 * <ul>
 *   <li>Présence de la clé {@code adventure-limits.wilderness-radius-per-level}.</li>
 *   <li>Exactement 11 entrées (niveaux 0 à 10, cohérent avec {@code jobs.levels}).</li>
 *   <li>Chaque valeur est un entier strictement positif.</li>
 *   <li>Les rayons sont croissants (un niveau supérieur doit ouvrir plus de territoire).</li>
 *   <li>Cohérence Nether : rayon niv.0 / 8 ≥ 1 (évite un rayon Nether nul).</li>
 * </ul>
 */
class AdventureLimitsConfigTest {

    @SuppressWarnings("unchecked")
    @Test
    void adventureLimitsSectionIsValid() throws IOException {
        Yaml yaml = new Yaml(new LoaderOptions());
        Map<String, Object> config;

        try (InputStream is = Files.newInputStream(Paths.get("src/main/resources/config.yml"))) {
            config = (Map<String, Object>) yaml.load(is);
        }

        assertNotNull(config, "config.yml ne doit pas être vide");

        // ── Présence de la section ─────────────────────────────────────────────────
        Map<String, Object> adventureLimits = (Map<String, Object>) config.get("adventure-limits");
        assertNotNull(adventureLimits,
                "La section 'adventure-limits' doit être présente dans config.yml");

        List<?> radiiRaw = (List<?>) adventureLimits.get("wilderness-radius-per-level");
        assertNotNull(radiiRaw,
                "La clé 'adventure-limits.wilderness-radius-per-level' doit être présente");

        // ── Nombre d'entrées = nombre de niveaux de métier ────────────────────────
        Map<String, Object> jobs = (Map<String, Object>) config.get("jobs");
        assertNotNull(jobs, "La section 'jobs' doit être présente");
        List<?> jobLevels = (List<?>) jobs.get("levels");
        assertNotNull(jobLevels, "La clé 'jobs.levels' doit être présente");

        assertEquals(jobLevels.size(), radiiRaw.size(),
                "wilderness-radius-per-level doit avoir exactement autant d'entrées que jobs.levels ("
                        + jobLevels.size() + " niveaux, 0 inclus)");

        // ── Valeurs valides et croissantes ────────────────────────────────────────
        int previous = 0;
        for (int i = 0; i < radiiRaw.size(); i++) {
            Object obj = radiiRaw.get(i);
            assertInstanceOf(Number.class, obj,
                    "wilderness-radius-per-level[" + i + "] doit être un nombre, trouvé : " + obj);

            int radius = ((Number) obj).intValue();
            assertTrue(radius > 0,
                    "wilderness-radius-per-level[" + i + "] doit être > 0, trouvé : " + radius);
            assertTrue(radius >= previous,
                    "wilderness-radius-per-level doit être croissant : niveau " + i
                            + " (" + radius + ") < niveau " + (i - 1) + " (" + previous + ")");
            previous = radius;
        }

        // ── Cohérence Nether : radius[0] / 8 doit rester ≥ 1 ────────────────────
        int minRadius = ((Number) radiiRaw.get(0)).intValue();
        assertTrue(minRadius / 8 >= 1,
                "Le rayon Nether au niveau 0 (wilderness-radius[0] / 8 = " + (minRadius / 8)
                        + ") serait nul ou négatif. Augmentez wilderness-radius[0] (actuellement "
                        + minRadius + ") à au moins 8.");
    }
}