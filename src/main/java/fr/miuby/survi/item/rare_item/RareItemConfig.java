package fr.miuby.survi.item.rare_item;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.system.log.ELogTag;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.EnumMap;
import java.util.logging.Level;

/**
 * Singleton qui charge et expose toutes les valeurs de {@code rare_items.yml}.
 * Rechargeable à chaud via {@link #reload(JavaPlugin)}.
 *
 * <p>Doit être initialisé via {@link #load(JavaPlugin)} avant la création de
 * {@link RareItemService} — donc dans {@code GameManager.initItems()}.</p>
 */
public class RareItemConfig {

    private static RareItemConfig instance;

    public static RareItemConfig getInstance() {
        if (instance == null) instance = new RareItemConfig();
        return instance;
    }

    private RareItemConfig() {}

    // ─── Paramètres globaux ───────────────────────────────────────────────────────

    @Getter private int    saveEvery;
    @Getter private int    minJobLevel;
    @Getter private long   suspiciousWindowMs;
    @Getter private double explorerMinDistanceSq;

    // ─── Paramètres par métier ────────────────────────────────────────────────────

    private final EnumMap<EJob, Long>   thresholds           = new EnumMap<>(EJob.class);
    private final EnumMap<EJob, Long>   growthRanges         = new EnumMap<>(EJob.class);
    private final EnumMap<EJob, Long>   suspiciousThresholds = new EnumMap<>(EJob.class);
    private final EnumMap<EJob, Double> maxChances           = new EnumMap<>(EJob.class);

    public long   getThreshold(EJob job)           { return thresholds.getOrDefault(job, 0L); }
    public long   getGrowthRange(EJob job)         { return growthRanges.getOrDefault(job, 5_000L); }
    public long   getSuspiciousThreshold(EJob job) { return suspiciousThresholds.getOrDefault(job, 100L); }
    public double getMaxChance(EJob job)           { return maxChances.getOrDefault(job, 0.0001); }

    // ─── Chargement ──────────────────────────────────────────────────────────────

    public void load(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "rare_items.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        saveEvery          = cfg.getInt   ("save-every",           25);
        minJobLevel        = cfg.getInt   ("min-job-level",        5);
        suspiciousWindowMs = cfg.getLong  ("suspicious-window-ms", 600_000L);

        thresholds.clear();
        growthRanges.clear();
        suspiciousThresholds.clear();
        maxChances.clear();

        for (EJob job : EJob.values()) {
            String key = "jobs." + job.name();
            thresholds          .put(job, cfg.getLong  (key + ".threshold",             0L));
            growthRanges        .put(job, cfg.getLong  (key + ".growth-range",      5_000L));
            suspiciousThresholds.put(job, cfg.getLong  (key + ".suspicious-threshold",  50L));
            if (cfg.contains(key + ".max-chance")) {
                maxChances.put(job, cfg.getDouble(key + ".max-chance"));
            }
        }

        double explorerMinDist = cfg.getDouble("jobs.EXPLORER.min-distance", 600.0);
        explorerMinDistanceSq  = explorerMinDist * explorerMinDist;

        MLLogManager.getInstance().log(Level.INFO, ELogTag.ITEM,
                "[RareItemConfig] Chargé — max-chance-par-job=" + maxChances.size()
                        + " save-every=" + saveEvery
                        + " min-job-level=" + minJobLevel
                        + " explorer-min-dist=" + explorerMinDist
                        + " suspicious-window-ms=" + suspiciousWindowMs);
    }

    public void reload(JavaPlugin plugin) {
        load(plugin);
    }
}