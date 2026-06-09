package fr.miuby.survi.job.config;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.log.ELogTag;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

/**
 * Charge {@code jobs.yml} et initialise le singleton {@link JobsConfig}.
 *
 * <p>À appeler dans {@code GameManager.init()} après {@code SurviConfig}
 * et après que {@code MLResourceManager.deploy(plugin, "jobs.yml")} a copié
 * le fichier dans le dossier du plugin.</p>
 *
 * <pre>{@code
 *   JobsLoader.load(plugin);
 * }</pre>
 */
public final class JobsLoader {

    private static final int LEVEL_COUNT = 11; // niveaux 0 à 10

    private JobsLoader() {}

    // ─── Point d'entrée ──────────────────────────────────────────────────────────

    public static void load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "jobs.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        double[] dropMult = readDouble(cfg, "drop-multiplier",
                new double[]{0.20, 0.50, 0.80, 1.00, 1.10, 1.20, 1.30, 1.40, 1.50, 1.75, 2.00});

        JobsConfig.LumberjackCfg lumberjack = new JobsConfig.LumberjackCfg(
                readDouble(cfg, "lumberjack.charcoal-chance",
                        new double[]{0, 0, 0, 0.15, 0.20, 0.25, 0.30, 0.35, 0.40, 0.45, 0.50}),
                readDouble(cfg, "lumberjack.apple-leaf-chance",
                        new double[]{0.001, 0.002, 0.003, 0.005, 0.055, 0.085, 0.115, 0.155, 0.205, 0.275, 0.355}),
                readDouble(cfg, "lumberjack.fire-damage-multiplier",
                        new double[]{2.00, 1.75, 1.50, 1.25, 1.10, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00}),
                readInt(cfg, "lumberjack.tree-feller-extra-logs",
                        new int[]{0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6}),
                readInt(cfg, "lumberjack.fire-resistance-ticks",
                        new int[]{0, 0, 0, 0, 0, 0, 0, 60, 100, 160, 240})
        );

        JobsConfig.FarmerCfg farmer = new JobsConfig.FarmerCfg(
                readDouble(cfg, "farmer.crop-growth-allow-chance",
                        new double[]{0.05, 0.15, 0.30, 0.50, 0.70, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00}),
                readDouble(cfg, "farmer.crop-extra-growth-chance",
                        new double[]{0, 0, 0, 0, 0, 0, 0.20, 0.40, 0.65, 1.00, 1.00}),
                cfg.getDouble("farmer.crop-third-tick-chance-at-max", 0.50)
        );

        JobsConfig.EnchanterCfg enchanter = new JobsConfig.EnchanterCfg(
                readDouble(cfg, "enchanter.durability-loss-multiplier",
                        new double[]{3.00, 2.00, 1.50, 1.25, 1.00, 0.75, 0.50, 0.25, 0.10, 0.00, 0.00}),
                readInt(cfg, "enchanter.anvil-max-xp-cost",
                        new int[]{2, 4, 7, 11, 16, 20, 25, 30, 35, 40, -1}),
                readInt(cfg, "enchanter.repair-per-xp",
                        new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 4})
        );

        JobsConfig.FishermanCfg fisherman = new JobsConfig.FishermanCfg(
                cfg.getInt("fisherman.vanilla-min-wait-ticks", 100),
                cfg.getInt("fisherman.vanilla-max-wait-ticks", 600),
                readDouble(cfg, "fisherman.fishing-wait-multiplier",
                        new double[]{2.00, 1.75, 1.50, 1.25, 1.10, 1.00, 0.85, 0.70, 0.55, 0.40, 0.25}),
                readDouble(cfg, "fisherman.loot-multiplier",
                        new double[]{0.20, 0.50, 0.80, 1.00, 1.10, 1.20, 1.30, 1.40, 1.50, 1.75, 2.00}),
                readDouble(cfg, "fisherman.dirt-chance",
                        new double[]{0.70, 0.56, 0.43, 0.30, 0.18, 0.08, 0.02, 0.00, 0.00, 0.00, 0.00}),
                readDouble(cfg, "fisherman.treasure-penalty",
                        new double[]{0.92, 0.78, 0.62, 0.44, 0.24, 0.08, 0.00, 0.00, 0.00, 0.00, 0.00}),
                readInt(cfg, "fisherman.pressure-safe-depth",
                        new int[]{0, 1, 5, 9, 20, 35, 55, 80, 110, 150, -1}),
                cfg.getDouble("fisherman.pressure-damage", 1.0),
                readInt(cfg, "fisherman.swim-speed-amplifier",
                        new int[]{0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1}),
                readInt(cfg, "fisherman.underwater-haste-amplifier",
                        new int[]{0, 0, 0, 0, 0, 0, 0, 1, 1, 2, 2}),
                cfg.getInt("fisherman.effect-duration-ticks", 25)
        );

        JobsConfig.ExplorerCfg explorer = new JobsConfig.ExplorerCfg(
                readDouble(cfg, "explorer.safe-fall-distance",
                        new double[]{0.7, 1.7, 2.7, 3.7, 4.7, 5.7, 6.7, 7.7, 8.7, 9.7, 10.7})
        );

        JobsConfig.setInstance(new JobsConfig(dropMult, lumberjack, farmer, enchanter, fisherman, explorer));
        MLLogManager.getInstance().log(Level.INFO, ELogTag.JOB, "[JobsLoader] jobs.yml chargé.");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

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
}