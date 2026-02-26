package fr.miuby.survi.system.log;

import com.google.common.base.CaseFormat;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.database.repository.SystemRepository;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestionnaire de logs par catégories ET par niveaux.
 *
 * FONCTIONNEMENT :
 * Un message s'affiche si :
 * - Son TAG est activé (VILLAGER, QUEST, etc.)
 * - ET son LEVEL est activé (INFO, WARNING, SEVERE)
 *
 * DÉFAUT :
 * - Tags : tous activés
 * - Levels : INFO désactivé, WARNING et SEVERE activés
 */
public class LogManager {
    private static LogManager instance = null;

    public static LogManager getInstance() {
        if (instance == null) {
            instance = new LogManager();
        }
        return instance;
    }

    @Getter
    private final Logger logger = Logger.getLogger("Survi");

    private final Map<ETagLog, Boolean> enabledTags = new HashMap<>();
    private final Map<Level, Boolean> enabledLevels = new HashMap<>();
    private boolean isInitialized = false;

    private LogManager() {
        // Valeurs par défaut AVANT chargement DB

        // Tags : tous activés par défaut
        for (ETagLog tag : ETagLog.values()) {
            enabledTags.put(tag, true);
        }

        // Levels : INFO désactivé, WARNING et SEVERE activés
        enabledLevels.put(Level.INFO, true);
        enabledLevels.put(Level.WARNING, true);
        enabledLevels.put(Level.SEVERE, true);
        enabledLevels.put(Level.CONFIG, true);
        enabledLevels.put(Level.FINE, true);
        enabledLevels.put(Level.FINER, true);
        enabledLevels.put(Level.FINEST, true);
    }

    public enum ETagLog {
        PLAYER,
        VILLAGER,
        QUEST,
        REPUTATION,
        ITEM,
        ROLE,
        WORLD,
        SYSTEM,
    }

    /**
     * Initialise le LogManager depuis la DB.
     */
    public void initialize() {
        if (isInitialized) {
            logger.warning("LogManager déjà initialisé !");
            return;
        }

        loadFromDatabase();
        isInitialized = true;

        // Log avec le système lui-même (si SYSTEM et INFO sont activés)
        log(Level.INFO, ETagLog.SYSTEM, "LogManager initialisé");
        log(Level.INFO, ETagLog.SYSTEM, "  ├─ Tags activés : " + countEnabled(enabledTags) + "/" + enabledTags.size());
        log(Level.INFO, ETagLog.SYSTEM, "  └─ Levels activés : " + countEnabled(enabledLevels) + "/" + enabledLevels.size());
    }

    /**
     * Charge les préférences depuis la DB.
     */
    private void loadFromDatabase() {
        SystemRepository repo = GameManager.getInstance().getDatabase().system();

        // Charge les tags
        for (ETagLog tag : ETagLog.values()) {
            Boolean state = repo.getLogTagState(tag.name());
            if (state != null) {
                enabledTags.put(tag, state);
            }
        }

        // Charge les levels
        for (Level level : enabledLevels.keySet()) {
            Boolean state = repo.getLogLevelState(level.getName());
            if (state != null) {
                enabledLevels.put(level, state);
            }
        }
    }

    private void saveTagToDatabase(ETagLog tag, boolean enabled) {
        GameManager.getInstance().getDatabase().system().saveLogTagState(tag.name(), enabled);
    }

    private void saveLevelToDatabase(Level level, boolean enabled) {
        GameManager.getInstance().getDatabase().system().saveLogLevelState(level.getName(), enabled);
    }

    /**
     * Log un message si le TAG et le LEVEL sont activés.
     */
    public void log(Level level, ETagLog tag, String message) {
        // Vérifie que le tag est activé
        if (!enabledTags.getOrDefault(tag, true)) {
            return;
        }

        // Vérifie que le level est activé
        if (!enabledLevels.getOrDefault(level, true)) {
            return;
        }

        // Formate et log
        String formattedTag = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, tag.toString());
        logger.log(level, "[" + formattedTag + "] " + message);
    }

    // ============================================================================
    // GESTION DES TAGS
    // ============================================================================

    /**
     * Toggle un tag (VILLAGER, QUEST, etc.).
     */
    public void toggleTag(ETagLog tag) {
        boolean newState = !enabledTags.getOrDefault(tag, true);
        enabledTags.put(tag, newState);
        saveTagToDatabase(tag, newState);
    }

    /**
     * Active/désactive un tag.
     */
    public void setTagEnabled(ETagLog tag, boolean enabled) {
        enabledTags.put(tag, enabled);
        saveTagToDatabase(tag, enabled);
    }

    /**
     * Vérifie si un tag est activé.
     */
    public boolean isTagEnabled(ETagLog tag) {
        return enabledTags.getOrDefault(tag, true);
    }

    /**
     * Active tous les tags.
     */
    public void enableAllTags() {
        for (ETagLog tag : ETagLog.values()) {
            setTagEnabled(tag, true);
        }
    }

    /**
     * Désactive tous les tags.
     */
    public void disableAllTags() {
        for (ETagLog tag : ETagLog.values()) {
            setTagEnabled(tag, false);
        }
    }

    /**
     * Retourne l'état de tous les tags.
     */
    public Map<ETagLog, Boolean> getAllTagStates() {
        return new HashMap<>(enabledTags);
    }

    // ============================================================================
    // GESTION DES LEVELS
    // ============================================================================

    /**
     * Toggle un level (INFO, WARNING, SEVERE).
     */
    public void toggleLevel(Level level) {
        boolean newState = !enabledLevels.getOrDefault(level, true);
        enabledLevels.put(level, newState);
        saveLevelToDatabase(level, newState);
    }

    /**
     * Active/désactive un level.
     */
    public void setLevelEnabled(Level level, boolean enabled) {
        enabledLevels.put(level, enabled);
        saveLevelToDatabase(level, enabled);
    }

    /**
     * Vérifie si un level est activé.
     */
    public boolean isLevelEnabled(Level level) {
        return enabledLevels.getOrDefault(level, true);
    }

    /**
     * Active tous les levels.
     */
    public void enableAllLevels() {
        for (Level level : enabledLevels.keySet()) {
            setLevelEnabled(level, true);
        }
    }

    /**
     * Désactive tous les levels.
     */
    public void disableAllLevels() {
        for (Level level : enabledLevels.keySet()) {
            setLevelEnabled(level, false);
        }
    }

    /**
     * Retourne l'état de tous les levels.
     */
    public Map<Level, Boolean> getAllLevelStates() {
        return new HashMap<>(enabledLevels);
    }

    // ============================================================================
    // PRESETS UTILES
    // ============================================================================

    /**
     * Mode PRODUCTION : seulement WARNING et SEVERE, tous les tags.
     */
    public void setProductionMode() {
        enableAllTags();

        setLevelEnabled(Level.INFO, false);
        setLevelEnabled(Level.CONFIG, false);
        setLevelEnabled(Level.FINE, false);
        setLevelEnabled(Level.FINER, false);
        setLevelEnabled(Level.FINEST, false);
        setLevelEnabled(Level.WARNING, true);
        setLevelEnabled(Level.SEVERE, true);

        logger.info("LogManager en mode PRODUCTION");
    }

    /**
     * Mode DEBUG : tout activé.
     */
    public void setDebugMode() {
        enableAllTags();
        enableAllLevels();
        logger.info("LogManager en mode DEBUG");
    }

    /**
     * Mode QUIET : seulement SEVERE.
     */
    public void setQuietMode() {
        enableAllTags();

        for (Level level : enabledLevels.keySet()) {
            setLevelEnabled(level, level == Level.SEVERE);
        }

        logger.severe("LogManager en mode QUIET");
    }

    // ============================================================================
    // UTILITAIRES
    // ============================================================================

    /**
     * Compte combien d'éléments sont activés dans une map.
     */
    private <T> long countEnabled(Map<T, Boolean> map) {
        return map.values().stream().filter(b -> b).count();
    }
}