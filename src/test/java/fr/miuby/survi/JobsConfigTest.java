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
 * Valide la structure et les plages de valeurs de {@code src/main/resources/jobs.yml}
 * sans runtime Bukkit.
 *
 * <h3>Ce qui est vérifié</h3>
 * <ul>
 *   <li>Présence du fichier et des sections racine.</li>
 *   <li>Chaque tableau a exactement 11 entrées (niveaux 0 à 10).</li>
 *   <li>Les tableaux de probabilités sont dans [0, 1].</li>
 *   <li>Les multiplicateurs et durées sont ≥ 0.</li>
 *   <li>Les scalaires critiques (vanilla-min/max-wait, effect-duration) sont > 0.</li>
 *   <li>Le schéma JSON est présent.</li>
 * </ul>
 */
@SuppressWarnings("unchecked")
class JobsConfigTest {

    private static final int LEVEL_COUNT = 11;

    private static Map<String, Object> root;

    @BeforeAll
    static void loadYaml() throws IOException {
        File file = new File("src/main/resources/jobs.yml");
        assertTrue(file.exists(), "jobs.yml introuvable dans src/main/resources/");
        try (FileInputStream fis = new FileInputStream(file)) {
            root = (Map<String, Object>) new Yaml(new LoaderOptions()).load(fis);
        }
        assertNotNull(root, "jobs.yml est vide ou mal formé");
    }

    // ─── Fichiers requis ─────────────────────────────────────────────────────────

    @Test
    void schemaExists() {
        assertTrue(new File("src/main/resources/schema/jobs-schema.json").exists(),
                "jobs-schema.json introuvable dans src/main/resources/schema/");
    }

    // ─── drop-multiplier ─────────────────────────────────────────────────────────

    @Test
    void dropMultiplierValid() {
        List<Number> list = getNumberList("drop-multiplier");
        assertSize(list, "drop-multiplier");
        for (int i = 0; i < LEVEL_COUNT; i++) {
            double v = list.get(i).doubleValue();
            assertTrue(v >= 0, "drop-multiplier[" + i + "] doit être ≥ 0, valeur : " + v);
        }
    }

    // ─── lumberjack ──────────────────────────────────────────────────────────────

    @Test
    void lumberjackCharcoalChanceValid() {
        assertProbabilityArray(getSection("lumberjack"), "charcoal-chance");
    }

    @Test
    void lumberjackAppleLeafChanceValid() {
        assertProbabilityArray(getSection("lumberjack"), "apple-leaf-chance");
    }

    @Test
    void lumberjackFireDamageMultiplierValid() {
        assertNonNegativeDoubleArray(getSection("lumberjack"), "fire-damage-multiplier");
    }

    @Test
    void lumberjackTreeFellerExtraLogsValid() {
        assertNonNegativeIntArray(getSection("lumberjack"), "tree-feller-extra-logs");
    }

    @Test
    void lumberjackFireResistanceTicksValid() {
        assertNonNegativeIntArray(getSection("lumberjack"), "fire-resistance-ticks");
    }

    // ─── farmer ──────────────────────────────────────────────────────────────────

    @Test
    void farmerCropGrowthAllowChanceValid() {
        assertProbabilityArray(getSection("farmer"), "crop-growth-allow-chance");
    }

    @Test
    void farmerCropExtraGrowthChanceValid() {
        assertProbabilityArray(getSection("farmer"), "crop-extra-growth-chance");
    }

    @Test
    void farmerCropThirdTickChanceValid() {
        Map<String, Object> farmer = getSection("farmer");
        assertTrue(farmer.containsKey("crop-third-tick-chance-at-max"),
                "farmer.crop-third-tick-chance-at-max manquant");
        double v = ((Number) farmer.get("crop-third-tick-chance-at-max")).doubleValue();
        assertTrue(v >= 0 && v <= 1,
                "farmer.crop-third-tick-chance-at-max doit être dans [0,1], valeur : " + v);
    }

    // ─── enchanter ───────────────────────────────────────────────────────────────

    @Test
    void enchanterDurabilityLossMultiplierValid() {
        assertNonNegativeDoubleArray(getSection("enchanter"), "durability-loss-multiplier");
    }

    @Test
    void enchanterAnvilMaxXpCostValid() {
        List<Number> list = getNumberList(getSection("enchanter"), "anvil-max-xp-cost");
        assertSize(list, "enchanter.anvil-max-xp-cost");
        for (int i = 0; i < LEVEL_COUNT; i++) {
            int v = list.get(i).intValue();
            assertTrue(v >= -1, "enchanter.anvil-max-xp-cost[" + i + "] doit être ≥ -1, valeur : " + v);
        }
    }

    @Test
    void enchanterRepairPerXpValid() {
        assertNonNegativeIntArray(getSection("enchanter"), "repair-per-xp");
    }

    // ─── fisherman ───────────────────────────────────────────────────────────────

    @Test
    void fishermanVanillaWaitTicksValid() {
        Map<String, Object> fish = getSection("fisherman");
        int min = ((Number) fish.get("vanilla-min-wait-ticks")).intValue();
        int max = ((Number) fish.get("vanilla-max-wait-ticks")).intValue();
        assertTrue(min > 0, "fisherman.vanilla-min-wait-ticks doit être > 0");
        assertTrue(max > min, "fisherman.vanilla-max-wait-ticks doit être > vanilla-min-wait-ticks");
    }

    @Test
    void fishermanFishingWaitMultiplierValid() {
        assertNonNegativeDoubleArray(getSection("fisherman"), "fishing-wait-multiplier");
    }

    @Test
    void fishermanLootMultiplierValid() {
        assertNonNegativeDoubleArray(getSection("fisherman"), "loot-multiplier");
    }

    @Test
    void fishermanDirtChanceValid() {
        assertProbabilityArray(getSection("fisherman"), "dirt-chance");
    }

    @Test
    void fishermanTreasurePenaltyValid() {
        assertProbabilityArray(getSection("fisherman"), "treasure-penalty");
    }

    @Test
    void fishermanPressureSafeDepthValid() {
        List<Number> list = getNumberList(getSection("fisherman"), "pressure-safe-depth");
        assertSize(list, "fisherman.pressure-safe-depth");
        for (int i = 0; i < LEVEL_COUNT; i++) {
            int v = list.get(i).intValue();
            assertTrue(v >= -1, "fisherman.pressure-safe-depth[" + i + "] doit être ≥ -1, valeur : " + v);
        }
    }

    @Test
    void fishermanPressureDamageValid() {
        Map<String, Object> fish = getSection("fisherman");
        double v = ((Number) fish.get("pressure-damage")).doubleValue();
        assertTrue(v >= 0, "fisherman.pressure-damage doit être ≥ 0, valeur : " + v);
    }

    @Test
    void fishermanSwimSpeedAmplifierValid() {
        assertNonNegativeIntArray(getSection("fisherman"), "swim-speed-amplifier");
    }

    @Test
    void fishermanUnderwaterHasteAmplifierValid() {
        assertNonNegativeIntArray(getSection("fisherman"), "underwater-haste-amplifier");
    }

    @Test
    void fishermanEffectDurationTicksValid() {
        int v = ((Number) getSection("fisherman").get("effect-duration-ticks")).intValue();
        assertTrue(v > 0, "fisherman.effect-duration-ticks doit être > 0, valeur : " + v);
    }

    // ─── explorer ──────────────────────────────────────────────────────────────

    @Test
    void explorerSafeFallDistanceValid() {
        List<Number> list = getNumberList(getSection("explorer"), "safe-fall-distance");
        assertSize(list, "explorer.safe-fall-distance");
        // Chaque niveau doit être > le précédent (progression croissante attendue)
        for (int i = 1; i < LEVEL_COUNT; i++) {
            double prev = list.get(i - 1).doubleValue();
            double curr = list.get(i).doubleValue();
            assertTrue(curr >= prev,
                    "explorer.safe-fall-distance[" + i + "] (" + curr +
                            ") ne doit pas être inférieur à l'index précédent (" + prev + ")");
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private Map<String, Object> getSection(String key) {
        Object val = root.get(key);
        assertNotNull(val, "Section racine manquante : " + key);
        assertInstanceOf(Map.class, val, "La section '" + key + "' n'est pas un objet YAML");
        return (Map<String, Object>) val;
    }

    private List<Number> getNumberList(String key) {
        return getNumberList(root, key);
    }

    private List<Number> getNumberList(Map<String, Object> section, String key) {
        Object val = section.get(key);
        assertNotNull(val, "Clé manquante : " + key);
        assertInstanceOf(List.class, val, "'" + key + "' n'est pas une liste");
        return (List<Number>) val;
    }

    private void assertSize(List<?> list, String key) {
        assertEquals(LEVEL_COUNT, list.size(),
                "'" + key + "' doit avoir " + LEVEL_COUNT + " entrées, en a : " + list.size());
    }

    private void assertProbabilityArray(Map<String, Object> section, String key) {
        List<Number> list = getNumberList(section, key);
        assertSize(list, key);
        for (int i = 0; i < LEVEL_COUNT; i++) {
            double v = list.get(i).doubleValue();
            assertTrue(v >= 0 && v <= 1,
                    key + "[" + i + "] doit être dans [0,1], valeur : " + v);
        }
    }

    private void assertNonNegativeDoubleArray(Map<String, Object> section, String key) {
        List<Number> list = getNumberList(section, key);
        assertSize(list, key);
        for (int i = 0; i < LEVEL_COUNT; i++) {
            double v = list.get(i).doubleValue();
            assertTrue(v >= 0, key + "[" + i + "] doit être ≥ 0, valeur : " + v);
        }
    }

    private void assertNonNegativeIntArray(Map<String, Object> section, String key) {
        List<Number> list = getNumberList(section, key);
        assertSize(list, key);
        for (int i = 0; i < LEVEL_COUNT; i++) {
            int v = list.get(i).intValue();
            assertTrue(v >= 0, key + "[" + i + "] doit être ≥ 0, valeur : " + v);
        }
    }
}