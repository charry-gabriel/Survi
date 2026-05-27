package fr.miuby.survi.system.log;

import com.google.common.base.CaseFormat;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.database.repository.SystemRepository;
import lombok.Getter;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.filter.AbstractFilter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Gestionnaire de logs par catégories ET par niveaux.
 *
 * FONCTIONNEMENT :
 * Un message s'affiche si :
 * - Son TAG est activé (VILLAGER, QUEST, etc.)
 * - ET son LEVEL est activé (INFO, WARNING, SEVERE)
 *
 * SORTIES :
 * - survi-debug-N.log  : tout (ALL)  — stacktraces incluses, 10 fichiers × 10 MB
 * - survi-info-N.log   : INFO+       — 5 fichiers × 5 MB
 * - survi-warn-N.log   : WARNING+    — 5 fichiers × 2 MB (archive légère)
 * - Console            : WARNING+ seulement
 *
 * Les messages parasites du serveur sont filtrés via suppressExternalNoise()
 * et redirigés dans debug uniquement.
 */
public class LogManager {
    private static LogManager instance = null;

    public static LogManager getInstance() {
        if (instance == null) instance = new LogManager();
        return instance;
    }

    @Getter
    private final Logger logger = Logger.getLogger("Survi");

    private final Map<ETagLog, Boolean> enabledTags = new HashMap<>();
    private final Map<Level, Boolean> enabledLevels = new HashMap<>();
    private boolean isInitialized = false;

    private LogManager() {
        for (ETagLog tag : ETagLog.values()) enabledTags.put(tag, true);

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
        JOB,
        WORLD,
        SYSTEM,
        GRAVE
    }

    // ============================================================================
    // INITIALISATION
    // ============================================================================

    public void initialize() {
        if (isInitialized) {
            logger.warning("LogManager déjà initialisé !");
            return;
        }

        setupHandlers();
        suppressExternalNoise();
        loadFromDatabase();
        isInitialized = true;

        log(Level.INFO, ETagLog.SYSTEM, "LogManager initialisé");
        log(Level.INFO, ETagLog.SYSTEM, "  ├─ Tags activés : " + countEnabled(enabledTags) + "/" + enabledTags.size());
        log(Level.INFO, ETagLog.SYSTEM, "  └─ Levels activés : " + countEnabled(enabledLevels) + "/" + enabledLevels.size());
    }

    // ============================================================================
    // SETUP HANDLERS
    // ============================================================================

    /**
     * Format : [2026-05-27 18:42:01] [INFO] message\nstacktrace si présente
     * %6$s est la throwable — vide si aucune, sinon retour à la ligne + stacktrace complète.
     */
    private static final String LOG_FORMAT = "[%1$tF %1$tT] [%4$s] %5$s%6$s%n";

    private void setupHandlers() {
        try {
            System.setProperty("java.util.logging.SimpleFormatter.format", LOG_FORMAT);

            File logDir = new File(GameManager.getInstance().getPlugin().getDataFolder(), "logs");
            logDir.mkdirs();

            // --- DEBUG : tout, stacktraces, longue rétention ---
            // 10 fichiers × 10 MB = 100 MB d'historique
            addFileHandler(logDir, "survi-debug-%g.log",
                    10 * 1024 * 1024, 10,
                    record -> true // tout passe
            );

            // --- INFO : INFO, WARNING, SEVERE ---
            // 5 fichiers × 5 MB
            addFileHandler(logDir, "survi-info-%g.log",
                    5 * 1024 * 1024, 5,
                    record -> record.getLevel().intValue() >= Level.INFO.intValue()
            );

            // --- WARN : WARNING et SEVERE seulement ---
            // 5 fichiers × 2 MB — archive légère pour production
            addFileHandler(logDir, "survi-warn-%g.log",
                    2 * 1024 * 1024, 5,
                    record -> record.getLevel().intValue() >= Level.WARNING.intValue()
            );

            // Débranche la console Bukkit
            logger.setUseParentHandlers(false);

            // Console : WARNING+ seulement
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new SimpleFormatter());
            consoleHandler.setLevel(Level.WARNING);
            logger.addHandler(consoleHandler);

            logger.setLevel(Level.ALL);

        } catch (IOException e) {
            logger.warning("Impossible de créer les fichiers de log : " + e.getMessage());
        }
    }

    private void addFileHandler(File logDir, String pattern, int maxBytes, int maxFiles, Filter filter)
            throws IOException {
        FileHandler handler = new FileHandler(
                new File(logDir, pattern).getPath(),
                maxBytes,
                maxFiles,
                true // append au redémarrage
        );
        handler.setFormatter(new SimpleFormatter());
        handler.setLevel(Level.ALL);
        handler.setFilter(filter);
        logger.addHandler(handler);
    }

    // ============================================================================
    // FILTRE BRUIT EXTERNE
    // ============================================================================

    private void suppressExternalNoise() {
        org.apache.logging.log4j.core.Logger rootLogger =
                (org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager.getRootLogger();

        rootLogger.addFilter(new AbstractFilter() {
            @Override
            public Result filter(LogEvent event) {
                String message = event.getMessage().getFormattedMessage();
                if (!isExternalNoise(message)) return Result.NEUTRAL;

                // Redirige dans debug uniquement (FINE → pas de console, pas de warn/info)
                logger.log(Level.FINE, "[External] {0}", message);
                return Result.DENY;
            }
        });
    }

    private boolean isExternalNoise(String message) {
        if (message == null) return false;
        return message.contains("Named entity")
                || message.contains("Saving oversized chunk");
    }

    // ============================================================================
    // LOG
    // ============================================================================

    /**
     * Log un message si le TAG et le LEVEL sont activés.
     */
    public void log(Level level, ETagLog tag, String message) {
        if (Boolean.FALSE.equals(enabledTags.getOrDefault(tag, true))) return;
        if (Boolean.FALSE.equals(enabledLevels.getOrDefault(level, true))) return;

        String formattedTag = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, tag.toString());
        logger.log(level, "[{0}] {1}", new Object[]{formattedTag, message});
    }

    /**
     * Log un message avec une exception (stacktrace complète dans le fichier debug).
     */
    public void log(Level level, ETagLog tag, String message, Throwable throwable) {
        if (Boolean.FALSE.equals(enabledTags.getOrDefault(tag, true))) return;
        if (Boolean.FALSE.equals(enabledLevels.getOrDefault(level, true))) return;

        String formattedTag = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, tag.toString());
        logger.log(level, "[" + formattedTag + "] " + message, throwable);
    }

    // ============================================================================
    // GESTION DES TAGS
    // ============================================================================

    public void toggleTag(ETagLog tag) {
        boolean newState = !enabledTags.getOrDefault(tag, true);
        enabledTags.put(tag, newState);
        saveTagToDatabase(tag, newState);
    }

    public void setTagEnabled(ETagLog tag, boolean enabled) {
        enabledTags.put(tag, enabled);
        saveTagToDatabase(tag, enabled);
    }

    public boolean isTagEnabled(ETagLog tag) {
        return enabledTags.getOrDefault(tag, true);
    }

    public void enableAllTags() {
        for (ETagLog tag : ETagLog.values()) setTagEnabled(tag, true);
    }

    public void disableAllTags() {
        for (ETagLog tag : ETagLog.values()) setTagEnabled(tag, false);
    }

    public Map<ETagLog, Boolean> getAllTagStates() {
        return new HashMap<>(enabledTags);
    }

    // ============================================================================
    // GESTION DES LEVELS
    // ============================================================================

    public void toggleLevel(Level level) {
        boolean newState = !enabledLevels.getOrDefault(level, true);
        enabledLevels.put(level, newState);
        saveLevelToDatabase(level, newState);
    }

    public void setLevelEnabled(Level level, boolean enabled) {
        enabledLevels.put(level, enabled);
        saveLevelToDatabase(level, enabled);
    }

    public boolean isLevelEnabled(Level level) {
        return enabledLevels.getOrDefault(level, true);
    }

    public void enableAllLevels() {
        for (Level level : enabledLevels.keySet()) setLevelEnabled(level, true);
    }

    public void disableAllLevels() {
        for (Level level : enabledLevels.keySet()) setLevelEnabled(level, false);
    }

    public Map<Level, Boolean> getAllLevelStates() {
        return new HashMap<>(enabledLevels);
    }

    // ============================================================================
    // PRESETS UTILES
    // ============================================================================

    /** Mode PRODUCTION : seulement WARNING et SEVERE, tous les tags. */
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

    /** Mode DEBUG : tout activé. */
    public void setDebugMode() {
        enableAllTags();
        enableAllLevels();
        logger.info("LogManager en mode DEBUG");
    }

    /** Mode QUIET : seulement SEVERE. */
    public void setQuietMode() {
        enableAllTags();
        for (Level level : enabledLevels.keySet()) setLevelEnabled(level, level == Level.SEVERE);
        logger.severe("LogManager en mode QUIET");
    }

    // ============================================================================
    // DB
    // ============================================================================

    private void loadFromDatabase() {
        SystemRepository repo = GameManager.getInstance().getDatabase().system();

        for (ETagLog tag : ETagLog.values()) {
            Boolean state = repo.getLogTagState(tag.name());
            if (state != null) enabledTags.put(tag, state);
        }

        for (Level level : enabledLevels.keySet()) {
            Boolean state = repo.getLogLevelState(level.getName());
            if (state != null) enabledLevels.put(level, state);
        }
    }

    private void saveTagToDatabase(ETagLog tag, boolean enabled) {
        GameManager.getInstance().getDatabase().system().saveLogTagState(tag.name(), enabled);
    }

    private void saveLevelToDatabase(Level level, boolean enabled) {
        GameManager.getInstance().getDatabase().system().saveLogLevelState(level.getName(), enabled);
    }

    // ============================================================================
    // UTILITAIRES
    // ============================================================================

    private <T> long countEnabled(Map<T, Boolean> map) {
        return map.values().stream().filter(b -> b).count();
    }
}