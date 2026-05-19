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

    /** Coordonnées du portail village pour un palier donné. */
    public record VillageZonePortal(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {}

    /** Spawn du village pour un palier donné. */
    public record VillageZoneSpawn(int x, int y, int z, float yaw, float pitch) {}

    /**
     * Un palier de zone village.
     * @param afterHours Nombre d'heures réelles après le début de partie à partir duquel ce palier s'active.
     * @param radius     Rayon autorisé en blocs depuis le centre.
     * @param spawn      Point de spawn du village pour ce palier.
     * @param portal     Position du portail village pour ce palier.
     */
    public record VillageZoneStage(float afterHours, int radius, VillageZoneSpawn spawn, VillageZonePortal portal) {}

    /**
     * Configuration complète de la zone village.
     * @param centerX Coordonnée X du centre du village.
     * @param centerZ Coordonnée Z du centre du village.
     * @param stages  Liste des paliers triés par {@code afterHours} croissant.
     */
    public record VillageZoneConfig(int centerX, int centerZ, List<VillageZoneStage> stages) {}

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

    @Getter private VillageZoneConfig villageZoneConfig;

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

        // ─── Zone Village ────────────────────────────────────────────────────────────
        int centerX = cfg.getInt("village-zone.center.x", 0);
        int centerZ = cfg.getInt("village-zone.center.z", 0);

        List<VillageZoneStage> zoneStages = new ArrayList<>();
        List<?> rawStages = cfg.getList("village-zone.stages");
        if (rawStages != null) {
            for (Object obj : rawStages) {
                if (obj instanceof java.util.Map<?, ?> stageMap) {
                    float afterHours = ((Number) stageMap.get("after-hours")).floatValue();
                    int  radius     = ((Number) stageMap.get("radius")).intValue();

                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> spawnMap =
                            (java.util.Map<String, Object>) stageMap.get("spawn");
                    VillageZoneSpawn spawn = new VillageZoneSpawn(
                            ((Number) spawnMap.get("x")).intValue(),
                            ((Number) spawnMap.get("y")).intValue(),
                            ((Number) spawnMap.get("z")).intValue(),
                            spawnMap.containsKey("yaw")   ? ((Number) spawnMap.get("yaw")).floatValue()   : 0f,
                            spawnMap.containsKey("pitch") ? ((Number) spawnMap.get("pitch")).floatValue() : 0f
                    );

                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> portalMap =
                            (java.util.Map<String, Object>) stageMap.get("portal");

                    VillageZonePortal portal = new VillageZonePortal(
                            ((Number) portalMap.get("min-x")).intValue(),
                            ((Number) portalMap.get("min-y")).intValue(),
                            ((Number) portalMap.get("min-z")).intValue(),
                            ((Number) portalMap.get("max-x")).intValue(),
                            ((Number) portalMap.get("max-y")).intValue(),
                            ((Number) portalMap.get("max-z")).intValue()
                    );
                    zoneStages.add(new VillageZoneStage(afterHours, radius, spawn, portal));
                }
            }
        }
        villageZoneConfig = new VillageZoneConfig(centerX, centerZ, zoneStages);

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.SYSTEM,
                "[SurviConfig] Configuration chargée (" + rankEntries.size() + " rangs, "
                        + jobLevelEntries.size() + " niveaux de métier, "
                        + zoneStages.size() + " paliers de zone village)");
    }
}