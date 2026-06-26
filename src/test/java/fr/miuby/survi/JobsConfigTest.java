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
 * Valide la structure et les plages de valeurs des fichiers {@code jobs/*.yml}
 * sans runtime Bukkit.
 *
 * <h3>Ce qui est vérifié</h3>
 * <ul>
 *   <li>Présence des 6 fichiers et de leurs schémas JSON associés.</li>
 *   <li>Chaque tableau a exactement 11 entrées (niveaux 0 à 10).</li>
 *   <li>Les tableaux de probabilités sont dans [0, 1].</li>
 *   <li>Les multiplicateurs et durées sont ≥ 0.</li>
 *   <li>Les listes de matériaux sont non-vides et contiennent des chaînes non-vides.</li>
 *   <li>Les scalaires critiques (vanilla-min/max-wait, effect-duration) sont > 0.</li>
 * </ul>
 */
@SuppressWarnings("unchecked")
class JobsConfigTest {

    private static final int LEVEL_COUNT = 11;

    private static Map<String, Object> minerRoot;
    private static Map<String, Object> lumberjackRoot;
    private static Map<String, Object> farmerRoot;
    private static Map<String, Object> enchanterRoot;
    private static Map<String, Object> fishermanRoot;
    private static Map<String, Object> explorerRoot;

    @BeforeAll
    static void loadYamls() throws IOException {
        minerRoot      = loadYamlFile("src/main/resources/jobs/miner.yml");
        lumberjackRoot = loadYamlFile("src/main/resources/jobs/lumberjack.yml");
        farmerRoot     = loadYamlFile("src/main/resources/jobs/farmer.yml");
        enchanterRoot  = loadYamlFile("src/main/resources/jobs/enchanter.yml");
        fishermanRoot  = loadYamlFile("src/main/resources/jobs/fisherman.yml");
        explorerRoot   = loadYamlFile("src/main/resources/jobs/explorer.yml");
    }

    private static Map<String, Object> loadYamlFile(String path) throws IOException {
        File file = new File(path);
        assertTrue(file.exists(), path + " introuvable dans src/main/resources/jobs/");
        try (FileInputStream fis = new FileInputStream(file)) {
            Map<String, Object> m = (Map<String, Object>) new Yaml(new LoaderOptions()).load(fis);
            assertNotNull(m, path + " est vide ou mal formé");
            return m;
        }
    }

    // ─── Schémas requis ──────────────────────────────────────────────────────────

    @Test
    void schemasExist() {
        for (String name : List.of("miner", "lumberjack", "farmer", "enchanter", "fisherman", "explorer")) {
            assertTrue(new File("src/main/resources/schema/jobs/" + name + "-schema.json").exists(),
                    "schema/jobs/" + name + "-schema.json introuvable");
        }
    }

    // ─── miner ───────────────────────────────────────────────────────────────────

    @Test
    void minerDropMultiplierValid() {
        assertNonNegativeDoubleArray(minerRoot, "drop-multiplier");
    }

    @Test
    void minerCaveNightVisionThresholdYValid() {
        List<Number> list = getNumberList(minerRoot, "cave-night-vision-threshold-y");
        assertSize(list, "miner.cave-night-vision-threshold-y");
        for (int i = 0; i < LEVEL_COUNT; i++) {
            int v = list.get(i).intValue();
            assertTrue(v == -1 || v >= -64,
                    "miner.cave-night-vision-threshold-y[" + i + "] invalide : " + v);
        }
        for (int i = 1; i < LEVEL_COUNT; i++) {
            int prev = list.get(i - 1).intValue();
            int curr = list.get(i).intValue();
            assertTrue(curr == -1 || curr <= prev,
                    "miner.cave-night-vision-threshold-y[" + i + "] (" + curr
                            + ") ne devrait pas dépasser l'index précédent (" + prev + ")");
        }
    }

    @Test
    void minerCaveDarknessThresholdYValid() {
        List<Number> list = getNumberList(minerRoot, "cave-darkness-threshold-y");
        assertSize(list, "miner.cave-darkness-threshold-y");
        for (int i = 0; i < LEVEL_COUNT; i++) {
            int v = list.get(i).intValue();
            assertTrue(v == -1 || v >= -64,
                    "miner.cave-darkness-threshold-y[" + i + "] invalide : " + v);
        }
        for (int i = 1; i < LEVEL_COUNT; i++) {
            int prev = list.get(i - 1).intValue();
            int curr = list.get(i).intValue();
            assertTrue(curr == -1 || curr <= prev,
                    "miner.cave-darkness-threshold-y[" + i + "] (" + curr
                            + ") ne devrait pas dépasser l'index précédent (" + prev + ")");
        }
    }

    @Test
    void minerNetherDarknessThresholdYValid() {
        List<Number> list = getNumberList(minerRoot, "nether-darkness-threshold-y");
        assertSize(list, "miner.nether-darkness-threshold-y");
        for (int i = 0; i < LEVEL_COUNT; i++) {
            int v = list.get(i).intValue();
            assertTrue(v == -1 || v >= -64,
                    "miner.nether-darkness-threshold-y[" + i + "] invalide : " + v);
        }
        for (int i = 1; i < LEVEL_COUNT; i++) {
            int prev = list.get(i - 1).intValue();
            int curr = list.get(i).intValue();
            assertTrue(curr == -1 || curr <= prev,
                    "miner.nether-darkness-threshold-y[" + i + "] (" + curr
                            + ") ne devrait pas dépasser l'index précédent (" + prev + ")");
        }
    }

    @Test
    void minerCaveDarknessHysteresisValid() {
        assertTrue(minerRoot.containsKey("cave-darkness-hysteresis"),
                "miner: cave-darkness-hysteresis manquant");
        int v = ((Number) minerRoot.get("cave-darkness-hysteresis")).intValue();
        assertTrue(v >= 0, "miner.cave-darkness-hysteresis doit être >= 0, valeur : " + v);
    }

    @Test
    void minerCaveNightVisionHysteresisValid() {
        assertTrue(minerRoot.containsKey("cave-night-vision-hysteresis"),
                "miner: cave-night-vision-hysteresis manquant");
        int v = ((Number) minerRoot.get("cave-night-vision-hysteresis")).intValue();
        assertTrue(v >= 0, "miner.cave-night-vision-hysteresis doit être >= 0, valeur : " + v);
    }

    @Test
    void minerCaveLightHysteresisValid() {
        assertTrue(minerRoot.containsKey("cave-light-hysteresis"),
                "miner: cave-light-hysteresis manquant");
        int v = ((Number) minerRoot.get("cave-light-hysteresis")).intValue();
        assertTrue(v >= 0 && v <= 15, "miner.cave-light-hysteresis doit être entre 0 et 15, valeur : " + v);
    }

    @Test void minerVeinMinerExtraOresValid() { assertNonNegativeIntArray(minerRoot, "vein-miner-extra-ores"); }

    // ─── lumberjack ──────────────────────────────────────────────────────────────

    @Test void lumberjackDropMultiplierValid()       { assertNonNegativeDoubleArray(lumberjackRoot, "drop-multiplier"); }
    @Test void lumberjackCharcoalChanceValid()       { assertProbabilityArray(lumberjackRoot, "charcoal-chance"); }
    @Test void lumberjackAppleLeafChanceValid()      { assertProbabilityArray(lumberjackRoot, "apple-leaf-chance"); }
    @Test void lumberjackFireDamageMultiplierValid() { assertNonNegativeDoubleArray(lumberjackRoot, "fire-damage-multiplier"); }
    @Test void lumberjackTreeFellerExtraLogsValid()  { assertNonNegativeIntArray(lumberjackRoot, "tree-feller-extra-logs"); }
    @Test void lumberjackFireResistanceTicksValid()  { assertNonNegativeIntArray(lumberjackRoot, "fire-resistance-ticks"); }

    // ─── farmer ──────────────────────────────────────────────────────────────────

    @Test void farmerDropMultiplierValid()        { assertNonNegativeDoubleArray(farmerRoot, "drop-multiplier"); }
    @Test void farmerCropGrowthAllowChanceValid() { assertProbabilityArray(farmerRoot, "crop-growth-allow-chance"); }
    @Test void farmerCropExtraGrowthChanceValid() { assertProbabilityArray(farmerRoot, "crop-extra-growth-chance"); }
    @Test void farmerBoneMealChanceValid()        { assertProbabilityArray(farmerRoot, "bone-meal-chance"); }

    @Test
    void farmerCropThirdTickChanceValid() {
        assertTrue(farmerRoot.containsKey("crop-third-tick-chance-at-max"),
                "farmer: crop-third-tick-chance-at-max manquant");
        double v = ((Number) farmerRoot.get("crop-third-tick-chance-at-max")).doubleValue();
        assertTrue(v >= 0 && v <= 1,
                "farmer.crop-third-tick-chance-at-max doit être dans [0,1], valeur : " + v);
    }

    // ─── enchanter ───────────────────────────────────────────────────────────────

    @Test void enchanterDurabilityLossMultiplierValid() { assertNonNegativeDoubleArray(enchanterRoot, "durability-loss-multiplier"); }

    @Test
    void enchanterAnvilMaxXpCostValid() {
        List<Number> list = getNumberList(enchanterRoot, "anvil-max-xp-cost");
        assertSize(list, "enchanter.anvil-max-xp-cost");
        for (int i = 0; i < LEVEL_COUNT; i++) {
            int v = list.get(i).intValue();
            assertTrue(v >= -1, "enchanter.anvil-max-xp-cost[" + i + "] doit être ≥ -1, valeur : " + v);
        }
    }

    @Test
    void enchanterEnchantMaxXpCostValid() {
        List<Number> list = getNumberList(enchanterRoot, "enchant-max-xp-cost");
        assertSize(list, "enchanter.enchant-max-xp-cost");
        for (int i = 0; i < LEVEL_COUNT; i++) {
            int v = list.get(i).intValue();
            assertTrue(v >= -1, "enchanter.enchant-max-xp-cost[" + i + "] doit être ≥ -1, valeur : " + v);
        }
    }

    @Test
    void enchanterEnchantMaxLevelValid() {
        List<Number> list = getNumberList(enchanterRoot, "enchant-max-level");
        assertSize(list, "enchanter.enchant-max-level");
        for (int i = 0; i < LEVEL_COUNT; i++) {
            int v = list.get(i).intValue();
            assertTrue(v >= -1, "enchanter.enchant-max-level[" + i + "] doit être ≥ -1, valeur : " + v);
        }
    }

    @Test void enchanterRepairPerXpValid() { assertNonNegativeIntArray(enchanterRoot, "repair-per-xp"); }

    // ─── fisherman ───────────────────────────────────────────────────────────────

    @Test
    void fishermanVanillaWaitTicksValid() {
        int min = ((Number) fishermanRoot.get("vanilla-min-wait-ticks")).intValue();
        int max = ((Number) fishermanRoot.get("vanilla-max-wait-ticks")).intValue();
        assertTrue(min > 0, "fisherman.vanilla-min-wait-ticks doit être > 0");
        assertTrue(max > min, "fisherman.vanilla-max-wait-ticks doit être > vanilla-min-wait-ticks");
    }

    @Test void fishermanFishingWaitMultiplierValid() { assertNonNegativeDoubleArray(fishermanRoot, "fishing-wait-multiplier"); }
    @Test void fishermanLootMultiplierValid()        { assertNonNegativeDoubleArray(fishermanRoot, "loot-multiplier"); }
    @Test void fishermanDirtChanceValid()            { assertProbabilityArray(fishermanRoot, "dirt-chance"); }
    @Test void fishermanTreasurePenaltyValid()       { assertProbabilityArray(fishermanRoot, "treasure-penalty"); }

    @Test
    void fishermanDirtReplacementMaterialsValid() {
        assertMaterialList(fishermanRoot, "dirt-replacement-materials");
    }

    @Test
    void fishermanTreasureReplacementMaterialsValid() {
        assertMaterialList(fishermanRoot, "treasure-replacement-materials");
    }

    @Test
    void fishermanPressureSafeDepthValid() {
        List<Number> list = getNumberList(fishermanRoot, "pressure-safe-depth");
        assertSize(list, "fisherman.pressure-safe-depth");
        for (int i = 0; i < LEVEL_COUNT; i++) {
            int v = list.get(i).intValue();
            assertTrue(v >= -500, "fisherman.pressure-safe-depth[" + i + "] valeur invalide : " + v);
        }
    }

    @Test
    void fishermanPressureDamageValid() {
        double v = ((Number) fishermanRoot.get("pressure-damage")).doubleValue();
        assertTrue(v >= 0, "fisherman.pressure-damage doit être ≥ 0, valeur : " + v);
    }

    @Test
    void fishermanUnderwaterSpeedModifierValid() {
        assertNonNegativeDoubleArray(fishermanRoot, "underwater-speed-modifier");
    }

    @Test
    void fishermanOxygenBonusValid() {
        List<Number> list = getNumberList(fishermanRoot, "oxygen-bonus");
        assertSize(list, "fisherman.oxygen-bonus");
        for (int i = 0; i < LEVEL_COUNT; i++) {
            double v = list.get(i).doubleValue();
            assertTrue(v >= 0.0, "fisherman.oxygen-bonus[" + i + "] doit être ≥ 0.0, valeur : " + v);
        }
        for (int i = 1; i < LEVEL_COUNT; i++) {
            double prev = list.get(i - 1).doubleValue();
            double curr = list.get(i).doubleValue();
            assertTrue(curr >= prev, "fisherman.oxygen-bonus[" + i + "] (" + curr + ") ne doit pas être inférieur à l'index précédent (" + prev + ")");
        }
    }

    @Test void fishermanSubmergedMiningSpeedModifierValid() { assertNonNegativeDoubleArray(fishermanRoot, "submerged-mining-speed-modifier"); }

    // ─── explorer ────────────────────────────────────────────────────────────────

    @Test
    void explorerSafeFallDistanceValid() {
        List<Number> list = getNumberList(explorerRoot, "safe-fall-distance");
        assertSize(list, "explorer.safe-fall-distance");
        for (int i = 1; i < LEVEL_COUNT; i++) {
            double prev = list.get(i - 1).doubleValue();
            double curr = list.get(i).doubleValue();
            assertTrue(curr >= prev,
                    "explorer.safe-fall-distance[" + i + "] (" + curr + ") ne doit pas être inférieur à l'index précédent (" + prev + ")");
        }
    }

    @Test
    void explorerWildernessRadiusPerLevelValid() {
        List<Number> list = getNumberList(explorerRoot, "wilderness-radius-per-level");
        assertSize(list, "explorer.wilderness-radius-per-level");
        int previous = 0;
        for (int i = 0; i < LEVEL_COUNT; i++) {
            int radius = list.get(i).intValue();
            assertTrue(radius > 0, "explorer.wilderness-radius-per-level[" + i + "] doit être > 0, valeur : " + radius);
            assertTrue(radius >= previous,
                    "explorer.wilderness-radius-per-level[" + i + "] (" + radius + ") ne doit pas être inférieur à l'index précédent (" + previous + ")");
            previous = radius;
        }

        // Cohérence Nether : radius[0] / 8 doit rester ≥ 1
        int minRadius = list.get(0).intValue();
        assertTrue(minRadius / 8 >= 1,
                "Le rayon Nether au niveau 0 (wilderness-radius-per-level[0] / 8 = " + (minRadius / 8)
                        + ") serait nul. Augmentez wilderness-radius-per-level[0] (actuellement " + minRadius + ") à au moins 8.");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

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

    /**
     * Vérifie qu'une clé est une liste non-vide de chaînes non-vides (noms de Material Minecraft).
     * La validation que le nom correspond à un Material existant est faite à l'exécution par JobsLoader
     * (nécessite le runtime Bukkit, non disponible en test unitaire).
     */
    private void assertMaterialList(Map<String, Object> section, String key) {
        Object val = section.get(key);
        assertNotNull(val, "Clé manquante : " + key);
        assertInstanceOf(List.class, val, "'" + key + "' n'est pas une liste");
        List<?> list = (List<?>) val;
        assertFalse(list.isEmpty(), key + " ne peut pas être vide (au moins 1 matériau requis)");
        for (int i = 0; i < list.size(); i++) {
            Object o = list.get(i);
            assertInstanceOf(String.class, o, key + "[" + i + "] doit être une chaîne (nom de Material Minecraft)");
            assertFalse(((String) o).isBlank(), key + "[" + i + "] ne peut pas être vide");
        }
    }
}