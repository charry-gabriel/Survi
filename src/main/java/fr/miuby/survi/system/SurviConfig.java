package fr.miuby.survi.system;

import fr.miuby.survi.player.GlobalRank;
import fr.miuby.survi.system.log.LogManager;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Singleton qui charge et expose toutes les valeurs du config.yml principal.
 *
 * <p>Doit être initialisé via {@link #init(JavaPlugin)} <em>avant</em> tout
 * autre composant qui y fait référence (TimeManager, WorldLevelManager, etc.)
 * — donc en tout premier dans {@code GameManager.init()}.
 */
public class SurviConfig {

    private static SurviConfig instance;

    public static SurviConfig getInstance() {
        if (instance == null) instance = new SurviConfig();
        return instance;
    }

    private SurviConfig() {}

    // ─── Modèles de données ──────────────────────────────────────────────────────

    public record RankEntry(String id, int threshold, String display) {}
    public record JobLevelEntry(int threshold, String name) {}

    // ─── Champs ──────────────────────────────────────────────────────────────────

    @Getter private int sunriseHour;
    @Getter private int sunsetHour;
    @Getter private int dailyResetHour;

    @Getter private float normalDamageModifier;
    @Getter private float normalResistanceModifier;
    @Getter private float endDamageModifier;
    @Getter private float endResistanceModifier;
    @Getter private int   deathLifeDivisor;

    @Getter private List<RankEntry>     rankEntries;
    @Getter private List<JobLevelEntry> jobLevelEntries;

    @Getter private double mobRarityBase;
    @Getter private double mobRarityPerLevel;
    @Getter private double mobRarityCap;
    @Getter private double mobDifficultyBase;
    @Getter private double mobDifficultyPerLevel;

    @Getter private int questCommonBase;
    @Getter private int questCommonMin;
    @Getter private int questCommonPerLevelPenalty;
    @Getter private int questLegendaryBase;
    @Getter private int questLegendaryMax;
    @Getter private int questLegendaryPerLevelGain;

    // ─── Initialisation ──────────────────────────────────────────────────────────

    public void init(JavaPlugin plugin) {
        FileConfiguration cfg = plugin.getConfig();

        sunriseHour    = cfg.getInt("time.sunrise-hour",     9);
        sunsetHour     = cfg.getInt("time.sunset-hour",      0);
        dailyResetHour = cfg.getInt("time.daily-reset-hour", 6);

        normalDamageModifier     = (float) cfg.getDouble("combat.normal.damage-modifier",     0.2);
        normalResistanceModifier = (float) cfg.getDouble("combat.normal.resistance-modifier", 0.2);
        endDamageModifier        = (float) cfg.getDouble("combat.end.damage-modifier",        0.7);
        endResistanceModifier    = (float) cfg.getDouble("combat.end.resistance-modifier",    0.7);
        deathLifeDivisor         = cfg.getInt("combat.death-life-divisor", 10);

        rankEntries = new ArrayList<>();
        List<?> rawRanks = cfg.getList("reputation.ranks");
        if (rawRanks != null) {
            for (Object obj : rawRanks) {
                if (obj instanceof java.util.Map<?, ?> map) {
                    rankEntries.add(new RankEntry(
                            String.valueOf(map.get("id")),
                            ((Number) map.get("threshold")).intValue(),
                            String.valueOf(map.get("display"))
                    ));
                }
            }
        }

        jobLevelEntries = new ArrayList<>();
        List<?> rawLevels = cfg.getList("jobs.levels");
        if (rawLevels != null) {
            for (Object obj : rawLevels) {
                if (obj instanceof java.util.Map<?, ?> map) {
                    jobLevelEntries.add(new JobLevelEntry(
                            ((Number) map.get("threshold")).intValue(),
                            String.valueOf(map.get("name"))
                    ));
                }
            }
        }

        mobRarityBase      = cfg.getDouble("world-level.mob-rarity.base",      1.0);
        mobRarityPerLevel  = cfg.getDouble("world-level.mob-rarity.per-level", 0.15);
        mobRarityCap       = cfg.getDouble("world-level.mob-rarity.cap",       5.0);
        mobDifficultyBase     = cfg.getDouble("world-level.mob-difficulty.base",      1.0);
        mobDifficultyPerLevel = cfg.getDouble("world-level.mob-difficulty.per-level", 0.05);

        questCommonBase            = cfg.getInt("world-level.quest-weights.common.base",              70);
        questCommonMin             = cfg.getInt("world-level.quest-weights.common.min",               30);
        questCommonPerLevelPenalty = cfg.getInt("world-level.quest-weights.common.per-level-penalty",  2);
        questLegendaryBase         = cfg.getInt("world-level.quest-weights.legendary.base",            5);
        questLegendaryMax          = cfg.getInt("world-level.quest-weights.legendary.max",            30);
        questLegendaryPerLevelGain = cfg.getInt("world-level.quest-weights.legendary.per-level-gain",  1);

        GlobalRank.initFromConfig(this);

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.SYSTEM,
                "[SurviConfig] Configuration chargée (" + rankEntries.size() + " rangs, "
                        + jobLevelEntries.size() + " niveaux de métier)");
    }
}
