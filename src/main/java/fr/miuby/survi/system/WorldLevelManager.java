package fr.miuby.survi.system;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.log.LogManager;

import java.util.logging.Level;

/**
 * Manages the server's global world level, which grows as players unlock
 * villager blessings and drives both mob difficulty and quest reward distribution.
 *
 * <p>All tuning values (base multipliers, caps, quest weight parameters) are
 * read from {@code config.yml} via {@link SurviConfig} — nothing is hardcoded here.
 *
 * <p>The world level is a single integer persisted in {@code server_data}
 * under the key {@code "world_level"} so it survives server restarts.
 */
public class WorldLevelManager {
    private static WorldLevelManager instance;

    public static WorldLevelManager getInstance() {
        if (instance == null) instance = new WorldLevelManager();
        return instance;
    }

    private WorldLevelManager() {}

    // ─── State ───────────────────────────────────────────────────────────────────

    private int worldLevel = 0;

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    public void load() {
        this.worldLevel = GameManager.getInstance().getDatabase().system().getWorldLevel();
        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.SYSTEM,
                "[WorldLevel] Niveau du monde chargé : " + worldLevel);
    }

    // ─── Mutation ────────────────────────────────────────────────────────────────

    public void increment(int delta) {
        if (delta <= 0) return;
        worldLevel += delta;
        persist();
        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.SYSTEM,
                "[WorldLevel] Niveau du monde : " + worldLevel + " (+" + delta + ")");
    }

    private void persist() {
        GameManager.getInstance().getDatabase().system().saveWorldLevel(worldLevel);
    }

    // ─── Accessors ───────────────────────────────────────────────────────────────

    public int getLevel() {
        return worldLevel;
    }

    /**
     * Multiplicateur de rareté des mobs.
     *
     * <p>Formule (config) : {@code min(cap, base + level × perLevel)}.
     */
    public double getMobRarityBoost() {
        SurviConfig cfg = SurviConfig.getInstance();
        return Math.min(cfg.getMobRarityCap(),
                cfg.getMobRarityBase() + worldLevel * cfg.getMobRarityPerLevel());
    }

    /**
     * Multiplicateur de difficulté des mobs (PV, dégâts).
     *
     * <p>Formule (config) : {@code base + level × perLevel} (sans plafond).
     */
    public double getMobDifficultyMultiplier() {
        SurviConfig cfg = SurviConfig.getInstance();
        return cfg.getMobDifficultyBase() + worldLevel * cfg.getMobDifficultyPerLevel();
    }

    /**
     * Poids de tirage des difficultés de quêtes {COMMON, RARE, LEGENDARY}.
     *
     * <p>Formules (config) :
     * <ul>
     *   <li>COMMON     : max(commonMin, commonBase − level × perLevelPenalty)</li>
     *   <li>LEGENDARY  : min(legendaryMax, legendaryBase + level × perLevelGain)</li>
     *   <li>RARE       : 100 − COMMON − LEGENDARY</li>
     * </ul>
     *
     * @return int[3] : [commonWeight, rareWeight, legendaryWeight]
     */
    public int[] getQuestDifficultyWeights() {
        SurviConfig cfg = SurviConfig.getInstance();
        int legendary = Math.min(cfg.getQuestLegendaryMax(),
                cfg.getQuestLegendaryBase() + worldLevel * cfg.getQuestLegendaryPerLevelGain());
        int common    = Math.max(cfg.getQuestCommonMin(),
                cfg.getQuestCommonBase() - worldLevel * cfg.getQuestCommonPerLevelPenalty());
        int rare      = 100 - common - legendary;
        return new int[]{common, rare, legendary};
    }

    public String getDisplayName() {
        return "Niveau " + worldLevel;
    }
}
