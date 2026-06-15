package fr.miuby.survi.system;

import fr.miuby.survi.player.EGlobalRank;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.config.VillageZoneConfig;
import fr.miuby.lib.log.MLLogManager;
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

    public record RankEntry(String id, int threshold, String display) {}
    public record JobLevelEntry(int threshold) {}

    // ─── Champs ──────────────────────────────────────────────────────────────────

    @Getter private int sunriseHour;
    @Getter private int sunsetHour;
    @Getter private int dailyResetHour;

    @Getter private float normalDamageModifier;
    @Getter private float normalResistanceModifier;
    @Getter private float endDamageModifier;
    @Getter private float endResistanceModifier;
    @Getter private int deathLifeDivisor;

    @Getter private int questCompletionReputation;

    @Getter private List<RankEntry> rankEntries;
    @Getter private List<JobLevelEntry> jobLevelEntries;

    @Getter private double mobRarityBase;
    @Getter private double mobRarityPerLevel;
    @Getter private double mobRarityCap;
    @Getter private double mobDifficultyBase;
    @Getter private double mobDifficultyPerLevel;

    @Getter private VillageZoneConfig villageZoneConfig;

    /** Rayon Wilderness (en blocs XZ) autorisé par niveau Explorateur (index 0–10). */
    @Getter private List<Integer> exploreWildernessRadius;

    // ─── Pluie ───────────────────────────────────────────────────────────────────

    /** Mondes dans lesquels le cycle pluie est géré par {@link fr.miuby.survi.world.RainManager}. */
    @Getter private List<EWorld> rainWorlds;
    /** Durée d'un épisode de pluie en secondes. */
    @Getter private int rainDurationSeconds;
    /** Borne basse du délai aléatoire entre deux pluies (secondes). */
    @Getter private int rainCooldownMinSeconds;
    /** Borne haute du délai aléatoire entre deux pluies (secondes). */
    @Getter private int rainCooldownMaxSeconds;

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
        questCompletionReputation = cfg.getInt("reputation.quest-completion-reputation", 10);
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
                            ((Number) map.get("threshold")).intValue()
                    ));
                }
            }
        }

        mobRarityBase      = cfg.getDouble("world-level.mob-rarity.base",      1.0);
        mobRarityPerLevel  = cfg.getDouble("world-level.mob-rarity.per-level", 0.15);
        mobRarityCap       = cfg.getDouble("world-level.mob-rarity.cap",       5.0);
        mobDifficultyBase     = cfg.getDouble("world-level.mob-difficulty.base",      1.0);
        mobDifficultyPerLevel = cfg.getDouble("world-level.mob-difficulty.per-level", 0.05);

        EGlobalRank.initFromConfig(this);

        // ─── Limites Explorateur ──────────────────────────────────────────────────────
        exploreWildernessRadius = new ArrayList<>();
        List<?> rawRadii = cfg.getList("explore-limits.wilderness-radius-per-level");
        if (rawRadii != null) {
            for (Object obj : rawRadii) {
                if (obj instanceof Number n) {
                    exploreWildernessRadius.add(n.intValue());
                }
            }
        }
        if (exploreWildernessRadius.isEmpty()) {
            exploreWildernessRadius = List.of(500, 750, 1000, 1500, 2000, 3000, 4000, 6000, 8000, 12000, 200000);
        }

        // ─── Zone Village ────────────────────────────────────────────────────────────
        int centerX = cfg.getInt("village-zone.center.x", 0);
        int centerZ = cfg.getInt("village-zone.center.z", 0);

        List<VillageZoneConfig.VillageZoneStage> zoneStages = new ArrayList<>();
        List<?> rawStages = cfg.getList("village-zone.stages");
        if (rawStages != null) {
            for (Object obj : rawStages) {
                if (obj instanceof java.util.Map<?, ?> stageMap) {
                    float afterHours = ((Number) stageMap.get("after-hours")).floatValue();
                    int  radius     = ((Number) stageMap.get("radius")).intValue();

                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> spawnMap = (java.util.Map<String, Object>) stageMap.get("spawn");
                    VillageZoneConfig.VillageZoneSpawn spawn = new VillageZoneConfig.VillageZoneSpawn(
                            ((Number) spawnMap.get("x")).intValue(),
                            ((Number) spawnMap.get("y")).intValue(),
                            ((Number) spawnMap.get("z")).intValue(),
                            spawnMap.containsKey("yaw")   ? ((Number) spawnMap.get("yaw")).floatValue()   : 0f,
                            spawnMap.containsKey("pitch") ? ((Number) spawnMap.get("pitch")).floatValue() : 0f
                    );

                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> portalMap = (java.util.Map<String, Object>) stageMap.get("portal");

                    VillageZoneConfig.VillageZonePortal portal = new VillageZoneConfig.VillageZonePortal(
                            ((Number) portalMap.get("min-x")).intValue(),
                            ((Number) portalMap.get("min-y")).intValue(),
                            ((Number) portalMap.get("min-z")).intValue(),
                            ((Number) portalMap.get("max-x")).intValue(),
                            ((Number) portalMap.get("max-y")).intValue(),
                            ((Number) portalMap.get("max-z")).intValue()
                    );
                    zoneStages.add(new VillageZoneConfig.VillageZoneStage(afterHours, radius, spawn, portal));
                }
            }
        }
        villageZoneConfig = new VillageZoneConfig(centerX, centerZ, zoneStages);

        // ─── Pluie ───────────────────────────────────────────────────────────────────
        rainDurationSeconds       = cfg.getInt("rain.duration-seconds",        60);
        rainCooldownMinSeconds    = cfg.getInt("rain.cooldown-min-seconds",     600);
        rainCooldownMaxSeconds    = cfg.getInt("rain.cooldown-max-seconds",    1500);

        rainWorlds = new ArrayList<>();
        List<?> rawRainWorlds = cfg.getList("rain.worlds");
        if (rawRainWorlds != null) {
            for (Object obj : rawRainWorlds) {
                try {
                    rainWorlds.add(EWorld.valueOf(String.valueOf(obj).toUpperCase()));
                } catch (IllegalArgumentException e) {
                    MLLogManager.getInstance().log(Level.WARNING, ELogTag.WORLD, "[SurviConfig] Monde pluie inconnu : " + obj);
                }
            }
        }
        if (rainWorlds.isEmpty()) rainWorlds = List.of(EWorld.WILDERNESS);

        MLLogManager.getInstance().log(Level.INFO, ELogTag.SYSTEM,
                "[SurviConfig] Configuration chargée (" + rankEntries.size() + " rangs, "
                        + jobLevelEntries.size() + " niveaux de métier, "
                        + zoneStages.size() + " paliers de zone village, "
                        + rainWorlds.size() + " monde(s) pluie)");
    }
}
