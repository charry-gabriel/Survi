package fr.miuby.survi.world;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.SurviConfig;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.log.ELogTag;

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
    public WorldLevelManager() {}

    // ─── State ───────────────────────────────────────────────────────────────────

    private int worldLevel = 0;

    // ─── Lifecycle ───────────────────────────────────────────────────────────────

    public void load() {
        this.worldLevel = GameManager.getInstance().getDatabase().system().getWorldLevel();
        MLLogManager.getInstance().log(Level.INFO, ELogTag.SYSTEM,
                "[WorldLevel] Niveau du monde chargé : " + worldLevel);
    }

    // ─── Mutation ────────────────────────────────────────────────────────────────

    public void increment(int delta) {
        if (delta <= 0) return;
        worldLevel += delta;
        persist();
        MLLogManager.getInstance().log(Level.INFO, ELogTag.SYSTEM,
                "[WorldLevel] Niveau du monde : " + worldLevel + " (+" + delta + ")");
    }

    /**
     * Remet le niveau du monde à zéro et persiste la valeur.
     * Réservé aux admins (tests, corrections manuelles).
     */
    public void reset() {
        worldLevel = 0;
        persist();
        MLLogManager.getInstance().log(Level.INFO, ELogTag.SYSTEM,
                "[WorldLevel] Niveau du monde réinitialisé à 0");
    }


    private void persist() {
        GameManager.getInstance().getDatabase().system().saveWorldLevel(worldLevel);
    }

    // ─── Accessors ───────────────────────────────────────────────────────────────

    public int getLevel() {
        return worldLevel;
    }

    public String getDisplayName() {
        return "Niveau " + worldLevel;
    }
}
