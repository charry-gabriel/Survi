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
 * Valide l'intégralité de {@code zone.yml} sans runtime Bukkit.
 *
 * <h3>Sections couvertes</h3>
 * <ul>
 *   <li>{@code stages} — liste non vide, triée par {@code after-hours} croissant.</li>
 *   <li>Premier palier : {@code after-hours = 0}.</li>
 *   <li>Chaque palier : {@code center-x/z} (entier), {@code half-width > 0}, {@code half-depth > 0},
 *       {@code spawn} valide, {@code portal} valide.</li>
 * </ul>
 */
class ZoneConfigTest {

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadZone() throws IOException {
        Yaml yaml = new Yaml(new LoaderOptions());
        try (InputStream is = Files.newInputStream(Paths.get("src/main/resources/zone.yml"))) {
            Map<String, Object> config = (Map<String, Object>) yaml.load(is);
            assertNotNull(config, "zone.yml ne doit pas être vide");
            return config;
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void stagesAreValid() throws IOException {
        Map<String, Object> config = loadZone();

        List<?> stages = (List<?>) config.get("stages");
        assertNotNull(stages, "zone.yml : 'stages' doit être présent");
        assertFalse(stages.isEmpty(), "zone.yml : 'stages' ne doit pas être vide");

        float previousAfterHours = -1f;
        for (int i = 0; i < stages.size(); i++) {
            Map<String, Object> stage = (Map<String, Object>) stages.get(i);
            String ctx = "stages[" + i + "]";
            assertNotNull(stage, ctx + " ne doit pas être null");

            // after-hours
            assertTrue(stage.containsKey("after-hours"), ctx + " : champ 'after-hours' manquant");
            assertInstanceOf(Number.class, stage.get("after-hours"), ctx + " : 'after-hours' doit être un nombre");
            float afterHours = ((Number) stage.get("after-hours")).floatValue();
            assertTrue(afterHours >= 0, ctx + " : 'after-hours' doit être ≥ 0, trouvé : " + afterHours);
            assertTrue(afterHours > previousAfterHours,
                    ctx + " : paliers non triés par 'after-hours' — précédent : " + previousAfterHours + ", actuel : " + afterHours);
            previousAfterHours = afterHours;

            // center
            assertIntField(stage, "center-x", ctx);
            assertIntField(stage, "center-z", ctx);

            // dimensions
            assertTrue(stage.containsKey("half-width"), ctx + " : champ 'half-width' manquant");
            assertInstanceOf(Integer.class, stage.get("half-width"), ctx + " : 'half-width' doit être un entier");
            assertTrue((Integer) stage.get("half-width") > 0, ctx + " : 'half-width' doit être > 0");

            assertTrue(stage.containsKey("half-depth"), ctx + " : champ 'half-depth' manquant");
            assertInstanceOf(Integer.class, stage.get("half-depth"), ctx + " : 'half-depth' doit être un entier");
            assertTrue((Integer) stage.get("half-depth") > 0, ctx + " : 'half-depth' doit être > 0");

            // spawn
            assertSpawnBlock(ctx, (Map<String, Object>) stage.get("spawn"));

            // portal
            assertPortalBlock(ctx, (Map<String, Object>) stage.get("portal"));
        }

        // Premier palier obligatoirement à after-hours = 0
        Map<?, ?> first = (Map<?, ?>) stages.get(0);
        assertEquals(0f, ((Number) first.get("after-hours")).floatValue(), 0.001f,
                "Le premier palier doit avoir after-hours = 0");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void assertSpawnBlock(String stageCtx, Map<String, Object> spawn) {
        String ctx = stageCtx + ".spawn";
        assertNotNull(spawn, ctx + " doit être présent");
        assertInstanceOf(Number.class, spawn.get("x"), ctx);
        assertInstanceOf(Number.class, spawn.get("y"), ctx);
        assertInstanceOf(Number.class, spawn.get("z"), ctx);
        if (spawn.containsKey("yaw"))   assertInstanceOf(Number.class, spawn.get("yaw"),   ctx + ".yaw doit être un nombre");
        if (spawn.containsKey("pitch")) assertInstanceOf(Number.class, spawn.get("pitch"), ctx + ".pitch doit être un nombre");
    }

    @SuppressWarnings("unchecked")
    private void assertPortalBlock(String stageCtx, Map<String, Object> portal) {
        String ctx = stageCtx + ".portal";
        assertNotNull(portal, ctx + " doit être présent");
        for (String coord : List.of("min-x", "min-y", "min-z", "max-x", "max-y", "max-z")) {
            assertIntField(portal, coord, ctx);
        }
    }

    private void assertIntField(Map<String, Object> section, String key, String ctx) {
        assertTrue(section.containsKey(key), ctx + "." + key + " doit être présent");
        assertInstanceOf(Integer.class, section.get(key), ctx + "." + key + " doit être un entier");
    }
}