package fr.miuby.survi.job.config;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.job.alchemic.AlchemicLootEntry;
import fr.miuby.survi.job.alchemic.ECustomPotion;
import fr.miuby.survi.system.log.ELogTag;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Charge les fichiers {@code jobs/*.yml} et initialise le singleton {@link JobsConfig}.
 *
 * <p>À appeler dans {@code GameManager.init()} après que
 * {@code MLResourceManager.deployFolder(plugin, "jobs")} a copié le dossier.</p>
 *
 * <pre>{@code
 *   JobsLoader.load(plugin);   // premier chargement
 *   JobsLoader.reload();       // rechargement à chaud (ex : /survi reload jobs)
 * }</pre>
 */
public final class JobsLoader {

    private static final int LEVEL_COUNT = 11; // niveaux 0 à 10

    /** Référence conservée pour permettre le rechargement sans repasser le plugin. */
    private static JavaPlugin cachedPlugin;

    private JobsLoader() {}

    // ─── Points d'entrée ─────────────────────────────────────────────────────────

    /** Premier chargement — stocke la référence du plugin pour les rechargements ultérieurs. */
    public static void load(JavaPlugin plugin) {
        cachedPlugin = plugin;
        reload();
    }

    /**
     * Rechargement à chaud de {@code jobs/*.yml}.
     * Requiert que {@link #load(JavaPlugin)} ait été appelé au moins une fois.
     */
    public static void reload() {
        if (cachedPlugin == null)
            throw new IllegalStateException("JobsLoader.load() doit être appelé avant reload().");
        File jobsDir = new File(cachedPlugin.getDataFolder(), "jobs");

        YamlConfiguration minerCfg      = loadFile(jobsDir, "miner.yml");
        YamlConfiguration lumberjackCfg = loadFile(jobsDir, "lumberjack.yml");
        YamlConfiguration farmerCfg     = loadFile(jobsDir, "farmer.yml");
        YamlConfiguration enchanterCfg  = loadFile(jobsDir, "enchanter.yml");
        YamlConfiguration fishermanCfg  = loadFile(jobsDir, "fisherman.yml");
        YamlConfiguration explorerCfg   = loadFile(jobsDir, "explorer.yml");

        JobsConfig.MinerCfg miner = new JobsConfig.MinerCfg(
                readDouble(minerCfg, "drop-multiplier",
                        new double[]{0.20, 0.50, 0.80, 1.00, 1.10, 1.20, 1.30, 1.40, 1.50, 1.75, 2.00}),
                readInt(minerCfg, "cave-night-vision-threshold-y",
                        new int[]{60, 50, 40, 25, 10, 0, -15, -30, -45, -55, -1}),
                readInt(minerCfg, "cave-darkness-threshold-y",
                        new int[]{30, 15, 5, -10, -25, -40, -50, -60, -64, -64, -1}),
                readInt(minerCfg, "nether-darkness-threshold-y",
                        new int[]{25, 25, 25, 25, 25, 25, 25, 25, 25, -1, -1}),
                minerCfg.getInt("cave-darkness-hysteresis", 3),
                minerCfg.getInt("cave-night-vision-hysteresis", 3),
                minerCfg.getInt("cave-light-hysteresis", 2),
                readInt(minerCfg, "vein-miner-extra-ores",
                        new int[]{0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6})
        );

        JobsConfig.LumberjackCfg lumberjack = new JobsConfig.LumberjackCfg(
                readDouble(lumberjackCfg, "drop-multiplier",
                        new double[]{0.20, 0.50, 0.80, 1.00, 1.10, 1.20, 1.30, 1.40, 1.50, 1.75, 2.00}),
                readDouble(lumberjackCfg, "charcoal-chance",
                        new double[]{0, 0, 0, 0.15, 0.20, 0.25, 0.30, 0.35, 0.40, 0.45, 0.50}),
                readDouble(lumberjackCfg, "apple-leaf-chance",
                        new double[]{0.001, 0.002, 0.003, 0.005, 0.055, 0.085, 0.115, 0.155, 0.205, 0.275, 0.355}),
                readDouble(lumberjackCfg, "fire-damage-multiplier",
                        new double[]{2.00, 1.75, 1.50, 1.25, 1.10, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00}),
                readInt(lumberjackCfg, "tree-feller-extra-logs",
                        new int[]{0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6}),
                readInt(lumberjackCfg, "fire-resistance-ticks",
                        new int[]{0, 0, 0, 0, 0, 0, 0, 60, 100, 160, 240}),
                readDouble(lumberjackCfg, "sapling-growth-allow-chance",
                        new double[]{0.05, 0.15, 0.30, 0.50, 0.70, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00}),
                readDouble(lumberjackCfg, "sapling-extra-growth-chance",
                        new double[]{0, 0, 0, 0, 0, 0, 0.20, 0.40, 0.65, 1.00, 1.00}),
                lumberjackCfg.getDouble("sapling-third-tick-chance-at-max", 0.50),
                readDouble(lumberjackCfg, "sapling-bone-meal-chance",
                        new double[]{0.01, 0.02, 0.10, 0.20, 0.30, 0.40, 0.50, 0.60, 0.70, 0.80, 1.00})
        );

        JobsConfig.FarmerCfg farmer = new JobsConfig.FarmerCfg(
                readDouble(farmerCfg, "drop-multiplier",
                        new double[]{0.20, 0.50, 0.80, 1.00, 1.10, 1.20, 1.30, 1.40, 1.50, 1.75, 2.00}),
                readDouble(farmerCfg, "crop-growth-allow-chance",
                        new double[]{0.05, 0.15, 0.30, 0.50, 0.70, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00}),
                readDouble(farmerCfg, "crop-extra-growth-chance",
                        new double[]{0, 0, 0, 0, 0, 0, 0.20, 0.40, 0.65, 1.00, 1.00}),
                farmerCfg.getDouble("crop-third-tick-chance-at-max", 0.50),
                readDouble(farmerCfg, "bone-meal-chance",
                        new double[]{0.05, 0.10, 0.20, 0.35, 0.50, 0.65, 0.75, 0.85, 0.90, 0.95, 1.00})
        );

        JobsConfig.EnchanterCfg enchanter = new JobsConfig.EnchanterCfg(
                readDouble(enchanterCfg, "durability-loss-multiplier",
                        new double[]{3.00, 2.00, 1.50, 1.25, 1.00, 0.75, 0.50, 0.25, 0.10, 0.00, 0.00}),
                readInt(enchanterCfg, "anvil-max-xp-cost",
                        new int[]{2, 4, 7, 11, 16, 20, 25, 30, 35, 40, -1}),
                readInt(enchanterCfg, "anvil-max-enchant-sum",
                        new int[]{0, 1, 3, 5, 6, 7, 9, 12, 15, 20, -1}),
                readInt(enchanterCfg, "enchant-max-xp-cost",
                        new int[]{0, 3, 6, 9, 12, 15, 18, 21, 24, 27, -1}),
                readInt(enchanterCfg, "enchant-max-level",
                        new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, -1}),
                readInt(enchanterCfg, "repair-per-xp",
                        new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 4})
        );

        Material[] defaultDirt = {Material.DIRT};
        JobsConfig.FishermanCfg fisherman = new JobsConfig.FishermanCfg(
                fishermanCfg.getInt("vanilla-min-wait-ticks", 100),
                fishermanCfg.getInt("vanilla-max-wait-ticks", 600),
                readDouble(fishermanCfg, "fishing-wait-multiplier",
                        new double[]{2.00, 1.75, 1.50, 1.25, 1.10, 1.00, 0.85, 0.70, 0.55, 0.40, 0.25}),
                readDouble(fishermanCfg, "loot-multiplier",
                        new double[]{0.20, 0.50, 0.80, 1.00, 1.10, 1.20, 1.30, 1.40, 1.50, 1.75, 2.00}),
                readDouble(fishermanCfg, "dirt-chance",
                        new double[]{0.70, 0.56, 0.43, 0.30, 0.18, 0.08, 0.02, 0.00, 0.00, 0.00, 0.00}),
                readMaterials(fishermanCfg, "dirt-replacement-materials", defaultDirt),
                readDouble(fishermanCfg, "treasure-penalty",
                        new double[]{0.92, 0.78, 0.62, 0.44, 0.24, 0.08, 0.00, 0.00, 0.00, 0.00, 0.00}),
                readMaterials(fishermanCfg, "treasure-replacement-materials", defaultDirt),
                readInt(fishermanCfg, "pressure-safe-depth",
                        new int[]{-500, 0, 2, 5, 10, 20, 50, 80, 110, 150, -1}),
                fishermanCfg.getDouble("pressure-damage", 1.0),
                readDouble(fishermanCfg, "underwater-speed-modifier",
                        new double[]{0.000, 0.000, 0.000, 0.010, 0.050, 0.100, 0.200, 0.400, 0.700, 1.000, 1.500}),
                readDouble(fishermanCfg, "oxygen-bonus",
                        new double[]{0.000, 0.200, 0.400, 0.667, 1.000, 1.600, 2.400, 4.000, 8.000, 14.000, 24.000}),
                readDouble(fishermanCfg, "submerged-mining-speed-modifier",
                        new double[]{0.000, 0.000, 0.000, 0.000, 0.000, 0.100, 0.200, 0.350, 0.500, 0.650, 0.800}),
                fishermanCfg.getDouble("acid-rain-damage", 1.0),
                fishermanCfg.getInt("acid-rain-fisherman-level-threshold", 5),
                readDouble(fishermanCfg, "alchemic-catch-chance",
                        new double[]{0.00, 0.03, 0.06, 0.10, 0.15, 0.22, 0.30, 0.38, 0.45, 0.52, 0.60}),
                readAlchemicLoot(fishermanCfg)
        );

        JobsConfig.ExplorerCfg explorer = new JobsConfig.ExplorerCfg(
                readDouble(explorerCfg, "safe-fall-distance",
                        new double[]{0.7, 1.7, 2.7, 3.7, 4.7, 5.7, 6.7, 7.7, 8.7, 9.7, 10.7}),
                readInt(explorerCfg, "wilderness-radius-per-level",
                        new int[]{100, 500, 1000, 2000, 4000, 8000, 15000, 25000, 50000, 100000, 2000000})
        );

        JobsConfig.setInstance(new JobsConfig(miner, lumberjack, farmer, enchanter, fisherman, explorer));
        MLLogManager.getInstance().log(Level.INFO, ELogTag.JOB, "[JobsLoader] jobs/*.yml rechargés.");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private static YamlConfiguration loadFile(File dir, String filename) {
        File file = new File(dir, filename);
        if (!file.exists())
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.JOB,
                    "[JobsLoader] Fichier manquant : jobs/" + filename + " — valeurs par défaut utilisées.");
        return YamlConfiguration.loadConfiguration(file); // retourne une config vide si absent
    }

    private static double[] readDouble(YamlConfiguration cfg, String key, double[] defaults) {
        List<?> list = cfg.getList(key);
        if (list == null || list.size() != LEVEL_COUNT) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.JOB,
                    "[JobsLoader] Clé manquante ou taille incorrecte : " + key + " — valeurs par défaut utilisées.");
            return defaults;
        }
        double[] result = new double[LEVEL_COUNT];
        for (int i = 0; i < LEVEL_COUNT; i++) {
            if (list.get(i) instanceof Number n) result[i] = n.doubleValue();
            else {
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.JOB,
                        "[JobsLoader] Valeur non numérique à l'index " + i + " de " + key + " — valeurs par défaut utilisées.");
                return defaults;
            }
        }
        return result;
    }

    private static int[] readInt(YamlConfiguration cfg, String key, int[] defaults) {
        List<?> list = cfg.getList(key);
        if (list == null || list.size() != LEVEL_COUNT) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.JOB,
                    "[JobsLoader] Clé manquante ou taille incorrecte : " + key + " — valeurs par défaut utilisées.");
            return defaults;
        }
        int[] result = new int[LEVEL_COUNT];
        for (int i = 0; i < LEVEL_COUNT; i++) {
            if (list.get(i) instanceof Number n) result[i] = n.intValue();
            else {
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.JOB,
                        "[JobsLoader] Valeur non numérique à l'index " + i + " de " + key + " — valeurs par défaut utilisées.");
                return defaults;
            }
        }
        return result;
    }

    /**
     * Lit une liste de noms de {@link Material} depuis le YAML.
     * Les noms invalides sont ignorés avec un warning. Si la liste résultante est vide, retourne {@code defaults}.
     */
    private static Material[] readMaterials(YamlConfiguration cfg, String key, Material[] defaults) {
        List<?> list = cfg.getList(key);
        if (list == null || list.isEmpty()) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.JOB,
                    "[JobsLoader] Clé manquante ou liste vide : " + key + " — valeurs par défaut utilisées.");
            return defaults;
        }
        List<Material> result = new ArrayList<>();
        for (Object o : list) {
            if (!(o instanceof String s)) continue;
            Material mat = Material.matchMaterial(s);
            if (mat != null) {
                result.add(mat);
            } else {
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.JOB,
                        "[JobsLoader] Matériau invalide dans " + key + " : « " + s + " » — ignoré.");
            }
        }
        if (result.isEmpty()) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.JOB,
                    "[JobsLoader] Aucun matériau valide dans " + key + " — valeurs par défaut utilisées.");
            return defaults;
        }
        return result.toArray(new Material[0]);
    }

    /**
     * Lit la table de loot alchimique depuis fisherman.yml.
     */
    private static List<AlchemicLootEntry> readAlchemicLoot(YamlConfiguration cfg) {
        List<?> rawList = cfg.getList("alchemic-loot");
        List<AlchemicLootEntry> result = new ArrayList<>();
        if (rawList == null || rawList.isEmpty()) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.JOB,
                    "[JobsLoader] alchemic-loot absent — table de loot alchimique vide.");
            return result;
        }
        for (Object raw : rawList) {
            if (!(raw instanceof Map<?, ?> map)) continue;
            String type  = getStr(map, "type", "");
            int levelMin = getNum(map, "level-min", 0);
            int weight   = getNum(map, "weight", 1);

            switch (type) {
                case "ingredient" -> {
                    Material mat = Material.matchMaterial(getStr(map, "item", ""));
                    if (mat == null) { warn("matériau inconnu", map, "item"); continue; }
                    result.add(new AlchemicLootEntry.IngredientEntry(mat, levelMin, weight));
                }
                case "vanilla_potion" -> {
                    PotionEffectType fx = PotionEffectType.getByName(getStr(map, "effect", ""));
                    if (fx == null) { warn("effet inconnu", map, "effect"); continue; }
                    result.add(new AlchemicLootEntry.VanillaPotionEntry(
                            fx, getNum(map, "duration", 600), getNum(map, "amplifier", 0),
                            getBool(map, "splash", false), levelMin, weight));
                }
                case "custom_potion" -> {
                    String id = getStr(map, "id", "");
                    try {
                        result.add(new AlchemicLootEntry.CustomPotionEntry(
                                ECustomPotion.valueOf(id), levelMin, weight));
                    } catch (IllegalArgumentException e) {
                        warn("potion custom inconnue", map, "id");
                    }
                }
                default -> MLLogManager.getInstance().log(Level.WARNING, ELogTag.JOB,
                        "[JobsLoader] alchemic-loot: type inconnu « " + type + " ».");
            }
        }
        return result;
    }

    private static void warn(String msg, Map<?, ?> map, String key) {
        MLLogManager.getInstance().log(Level.WARNING, ELogTag.JOB,
                "[JobsLoader] alchemic-loot: " + msg + " — valeur: « " + map.get(key) + " ».");
    }

    private static String getStr(Map<?, ?> map, String key, String def) {
        Object v = map.get(key); return v instanceof String s ? s : def;
    }
    private static int getNum(Map<?, ?> map, String key, int def) {
        Object v = map.get(key); return v instanceof Number n ? n.intValue() : def;
    }
    private static boolean getBool(Map<?, ?> map, String key, boolean def) {
        Object v = map.get(key); return v instanceof Boolean b ? b : def;
    }
}