package fr.miuby.survi.world;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.log.LogManager;

import java.util.logging.Level;

/**
 * Manages the server's global world level, which grows as players unlock
 * villager blessings and drives both mob difficulty and quest reward distribution.
 *
 * <p>The world level is a single integer that acts as the source of truth for:
 * <ul>
 *   <li>Mob rarity weight boost — higher level shifts rolls toward rarer classes.</li>
 *   <li>Mob stat multiplier — higher level increases HP / damage of all classed mobs.</li>
 *   <li>Quest difficulty distribution — higher level raises the share of RARE and LEGENDARY quests.</li>
 * </ul>
 *
 * <p>The value is persisted in {@code server_data} under the key {@code "world_level"}
 * so it survives server restarts.
 */
public class WorldLevelManager {
    private static WorldLevelManager instance;

    public static WorldLevelManager getInstance() {
        if (instance == null) instance = new WorldLevelManager();
        return instance;
    }

    private WorldLevelManager() {}

    // ─── State ───────────────────────────────────────────────────────────────────

    /** Current world level. Starts at 0, has no hard cap. */
    private int worldLevel = 0;

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    /**
     * Loads the world level from the database.
     * Must be called after the database is fully initialised (post {@code initDatabase()}).
     */
    public void load() {
        this.worldLevel = GameManager.getInstance().getDatabase().system().getWorldLevel();
        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.SYSTEM, "[WorldLevel] Niveau du monde chargé : " + worldLevel);
    }

    // ─── Mutation ────────────────────────────────────────────────────────────────

    /**
     * Increments the world level by {@code delta} and persists the new value.
     *
     * @param delta positive integer; usually 1 per blessing applied
     */
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

    /** Raw world level value. */
    public int getLevel() {
        return worldLevel;
    }

    /**
     * Multiplier to boost higher-rarity weights.
     *
     * <p>Formula: {@code 1.0 + level × 0.15}, capped at {@code 5.0}.
     * Level 0 → 1.0 (no boost), level 10 → 2.5, level 27+ → 5.0 (cap).
     */
    public double getMobRarityBoost() {
        return Math.min(5.0, 1.0 + worldLevel * 0.15);
    }

    /**
     * Multiplier applied to mob stats (HP, damage)
     *
     * <p>Formula: {@code 1.0 + level × 0.05}.
     * Level 0 → 1.0, level 10 → 1.5, level 20 → 2.0.
     */
    public double getMobDifficultyMultiplier() {
        return 1.0 + worldLevel * 0.05;
    }

    /**
     * Returns the three quest-difficulty weights {COMMON, RARE, LEGENDARY}
     * used by {@link fr.miuby.survi.quest.QuestManager#getRandomDifficulty()}.
     *
     * <p>At level 0 this reproduces the legacy odds (70 / 25 / 5).
     * As the level grows, weight shifts toward harder difficulties:
     * <ul>
     *   <li>COMMON  : max(30, 70 − level × 2)</li>
     *   <li>LEGENDARY: min(30,  5 + level)</li>
     *   <li>RARE    : remainder</li>
     * </ul>
     *
     * @return int array of length 3: [commonWeight, rareWeight, legendaryWeight]
     */
    public int[] getQuestDifficultyWeights() {
        int legendary = Math.min(30, 5 + worldLevel);
        int common    = Math.max(30, 70 - worldLevel * 2);
        int rare      = 100 - common - legendary;
        return new int[]{common, rare, legendary};
    }

    /**
     * Short display string for the Tab list / HUD.
     * Example: "Niveau 7"
     */
    public String getDisplayName() {
        return "Niveau " + worldLevel;
    }
}