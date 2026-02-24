package fr.miuby.survi.system.time;

import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.log.LogManager;
import fr.miuby.survi.system.time.event.DailyResetEvent;
import fr.miuby.survi.world.EWorld;
import lombok.Getter;
import org.bukkit.scheduler.BukkitTask;

import java.time.*;
import java.util.logging.Level;

/**
 * Gère le système de temps du serveur :
 * 1. Synchronise le temps Minecraft du Village avec l'heure réelle
 * 2. Déclenche un reset quotidien à 6h du matin
 *
 * COMMENT ÇA MARCHE :
 *
 * === SYNCHRO TEMPS RÉEL → MINECRAFT ===
 * Le Village suit l'heure réelle avec cette logique simple :
 * - 0h réelle   = 0 ticks Minecraft (lever du soleil)
 * - 8h réelle   = 12000 ticks (coucher du soleil)
 * - 24h réelle  = 24000 ticks (retour au lever du soleil)
 *
 * === RESET QUOTIDIEN ===
 * À 6h du matin chaque jour :
 * - Vérifie qu'on a pas déjà reset aujourd'hui
 * - Déclenche un DailyResetEvent
 * - Les systèmes (quêtes, buffs, etc.) écoutent cet event et se resetent
 * - Persiste l'état en DB pour détecter les resets manqués
 */
public class TimeManager {
    // === CONFIGURATION ===
    private static final int RESET_HOUR = 6;         // 6h du matin
    private static final int REAL_SUNRISE = 0;       // 0h réelle = lever du soleil MC
    private static final int REAL_SUNSET = 8;        // 8h réelle = coucher du soleil MC
    private static final int SYNC_INTERVAL_TICKS = 20;      // Update temps chaque seconde
    private static final int RESET_CHECK_INTERVAL_TICKS = 20 * 60;  // Vérifie reset chaque minute

    // === CONSTANTES MINECRAFT ===
    // Un jour Minecraft complet = 24000 ticks (20 minutes réelles)
    // 0 ticks = 6h matin MC, 6000 = midi, 12000 = 18h soir, 18000 = minuit
    private static final int MC_DAY_START = 0;       // Lever du soleil
    private static final int MC_SUNSET = 12000;      // Coucher du soleil
    private static final int MC_FULL_DAY = 24000;    // Jour complet

    private final LogManager logger = LogManager.getInstance();
    @Getter
    private final ZoneId serverTimezone;

    private BukkitTask syncTask;
    private BukkitTask resetCheckTask;

    @Getter
    private long lastResetTimestamp;  // Timestamp du dernier reset
    @Getter
    private int lastResetDay;         // Jour depuis epoch du dernier reset

    public TimeManager() {
        this.serverTimezone = ZoneId.systemDefault();
        loadLastReset();
    }

    /**
     * Démarre les deux systèmes :
     * 1. Synchronisation temps réel → Minecraft (chaque seconde)
     * 2. Vérification du reset quotidien (chaque minute)
     */
    public void start() {
        // Vérifie immédiatement si on a manqué un reset
        checkForMissedReset();

        // Démarre la synchro temps réel → Minecraft
        this.syncTask = GameManager.getInstance().getScheduler().runTaskTimer(
                GameManager.getInstance().getPlugin(),
                this::updateMinecraftTime,
                0L,
                SYNC_INTERVAL_TICKS
        );

        // Démarre la vérification du reset quotidien
        this.resetCheckTask = GameManager.getInstance().getScheduler().runTaskTimer(
                GameManager.getInstance().getPlugin(),
                this::checkForReset,
                20L * 60,  // Attend 1 minute avant de commencer
                RESET_CHECK_INTERVAL_TICKS
        );

        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM, "TimeManager démarré");
        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM, "  ├─ Timezone: " + serverTimezone.getId());
        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM, "  ├─ Village: synchro temps réel ACTIVE");
        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM, "  ├─ Reset quotidien: " + RESET_HOUR + "h00");
        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM, "  └─ Prochain reset: " + getNextResetTime().format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
    }

    /**
     * Arrête le système proprement (sauvegarde l'état).
     */
    public void stop() {
        if (syncTask != null) {
            syncTask.cancel();
            syncTask = null;
        }
        if (resetCheckTask != null) {
            resetCheckTask.cancel();
            resetCheckTask = null;
        }
        saveLastReset();
        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"TimeManager arrêté");
    }

    // ============================================================================
    // PARTIE 1 : SYNCHRONISATION TEMPS RÉEL → MINECRAFT
    // ============================================================================

    /**
     * Met à jour le temps Minecraft du Village selon l'heure réelle.
     * Appelé chaque seconde par la task.
     *
     * PRINCIPE :
     * L'heure réelle est convertie en ticks Minecraft avec une progression linéaire.
     * De 0h à 8h : jour Minecraft (0 → 12000 ticks)
     * De 8h à 24h : nuit Minecraft (12000 → 24000 → 0 ticks)
     */
    private void updateMinecraftTime() {
        ZonedDateTime now = ZonedDateTime.now(serverTimezone);

        // Secondes écoulées depuis minuit (0h00)
        long secondsSinceMidnight = now.toLocalTime().toSecondOfDay();

        // Calcule les ticks Minecraft correspondants
        int minecraftTicks = calculateMinecraftTicks(secondsSinceMidnight);

        // Détermine si c'est la nuit (après 8h réelles)
        boolean isNight = secondsSinceMidnight >= (REAL_SUNSET * 3600);

        // Applique au monde Village
        WorldRegistry.get(EWorld.VILLAGE).getWorld().setTime(minecraftTicks);
        GameManager.getInstance().setNight(isNight);
    }

    /**
     * Calcule les ticks Minecraft selon l'heure réelle.
     *
     * FORMULE CLAIRE ET COMMENTÉE :
     *
     * Phase 1 - JOUR (0h → 8h réelles) :
     *   Progression : 0% à 100% du lever au coucher du soleil
     *   Ticks : 0 → 12000
     *   Formule : (secondes écoulées / secondes totales du jour) × 12000
     *
     * Phase 2 - NUIT (8h → 24h réelles) :
     *   Progression : 0% à 100% du coucher au lever du soleil
     *   Ticks : 12000 → 24000 (puis retour à 0)
     *   Formule : 12000 + ((secondes depuis coucher / secondes totales nuit) × 12000)
     *
     * @param secondsSinceMidnight Secondes écoulées depuis minuit (0-86400)
     * @return Ticks Minecraft (0-24000)
     */
    private int calculateMinecraftTicks(long secondsSinceMidnight) {
        // Conversion heures → secondes
        int sunriseSeconds = REAL_SUNRISE * 3600;  // 0h = 0 secondes
        int sunsetSeconds = REAL_SUNSET * 3600;    // 8h = 28800 secondes
        int midnightSeconds = 24 * 3600;           // 24h = 86400 secondes

        if (secondsSinceMidnight < sunsetSeconds) {
            // === PHASE JOUR (0h-8h) ===
            // Exemple : 4h du matin (14400 secondes)
            //   progress = 14400 / 28800 = 0.5 (50%)
            //   ticks = 0.5 × 12000 = 6000 (midi Minecraft)
            double progress = (double) secondsSinceMidnight / sunsetSeconds;
            return (int) (progress * MC_SUNSET);

        } else {
            // === PHASE NUIT (8h-24h) ===
            // Exemple : 20h du soir (72000 secondes)
            //   nightSeconds = 72000 - 28800 = 43200
            //   totalNightSeconds = 86400 - 28800 = 57600
            //   progress = 43200 / 57600 = 0.75 (75% de la nuit)
            //   ticks = 12000 + (0.75 × 12000) = 21000
            long nightSeconds = secondsSinceMidnight - sunsetSeconds;
            long totalNightSeconds = midnightSeconds - sunsetSeconds;
            double progress = (double) nightSeconds / totalNightSeconds;
            return MC_SUNSET + (int) (progress * (MC_FULL_DAY - MC_SUNSET));
        }
    }

    // ============================================================================
    // PARTIE 2 : RESET QUOTIDIEN À 6H
    // ============================================================================

    /**
     * Vérifie si on doit faire un reset maintenant.
     * Appelé chaque minute par la task.
     *
     * CONDITIONS pour reset :
     * 1. On est un jour différent du dernier reset
     * 2. Il est au moins 6h du matin
     */
    private void checkForReset() {
        ZonedDateTime now = ZonedDateTime.now(serverTimezone);
        int currentDay = getCurrentDay(now);

        if (currentDay > lastResetDay && now.getHour() >= RESET_HOUR) {
            performDailyReset(now);
        }
    }

    /**
     * Vérifie si on a manqué un reset (serveur éteint pendant plusieurs jours).
     * Appelé au démarrage.
     *
     * SCÉNARIO :
     * - Dernier reset : 17 février à 6h
     * - Serveur redémarre : 20 février à 14h
     * → On a manqué 3 jours de reset, on reset immédiatement
     */
    private void checkForMissedReset() {
        ZonedDateTime now = ZonedDateTime.now(serverTimezone);
        int currentDay = getCurrentDay(now);

        if (currentDay > lastResetDay && now.getHour() >= RESET_HOUR) {
            logger.log(Level.WARNING, LogManager.ETagLog.SYSTEM,"╔════════════════════════════════════╗");
            logger.log(Level.WARNING, LogManager.ETagLog.SYSTEM,"║   RESET MANQUÉ DÉTECTÉ !           ║");
            logger.log(Level.WARNING, LogManager.ETagLog.SYSTEM,"╠════════════════════════════════════╣");
            logger.log(Level.WARNING, LogManager.ETagLog.SYSTEM,"║ Dernier reset : jour " + lastResetDay + "          ║");
            logger.log(Level.WARNING, LogManager.ETagLog.SYSTEM,"║ Jour actuel   : jour " + currentDay + "          ║");
            logger.log(Level.WARNING, LogManager.ETagLog.SYSTEM,"║ → Reset immédiat déclenché         ║");
            logger.log(Level.WARNING, LogManager.ETagLog.SYSTEM,"╚════════════════════════════════════╝");
            performDailyReset(now);
        } else {
            logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"Aucun reset manqué (dernier reset : jour " + lastResetDay + ")");
        }
    }

    /**
     * Effectue le reset quotidien.
     *
     * ACTIONS :
     * 1. Met à jour lastResetDay et lastResetTimestamp
     * 2. Sauvegarde en DB
     * 3. Déclenche DailyResetEvent pour tous les systèmes
     */
    private void performDailyReset(ZonedDateTime now) {
        int currentDay = getCurrentDay(now);
        long timestamp = now.toInstant().toEpochMilli();

        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"╔═══════════════════════════════════════════════╗");
        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"║         RESET QUOTIDIEN - 6H00                ║");
        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"╠═══════════════════════════════════════════════╣");
        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"║ Jour : " + currentDay + " (précédent : " + lastResetDay + ")");
        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"║ Heure : " + now.format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm:ss")));
        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"╚═══════════════════════════════════════════════╝");

        // Update l'état
        this.lastResetTimestamp = timestamp;
        this.lastResetDay = currentDay;
        saveLastReset();

        // Déclenche l'event pour tous les systèmes
        DailyResetEvent event = new DailyResetEvent(timestamp, currentDay);
        GameManager.getInstance().callEvent(event);

        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"Reset quotidien terminé !");
    }

    /**
     * Force un reset manuel (pour admin/debug).
     * Commande : /time forcereset
     */
    public void forceReset() {
        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"╔═══════════════════════════════════════════════╗");
        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"║      RESET FORCÉ MANUELLEMENT                 ║");
        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"╚═══════════════════════════════════════════════╝");
        performDailyReset(ZonedDateTime.now(serverTimezone));
    }

    // ============================================================================
    // UTILITAIRES - INFO ET CALCULS
    // ============================================================================

    /**
     * Retourne le nombre de jours depuis l'epoch (01/01/1970).
     * Utilisé pour comparer les jours entre eux.
     */
    private int getCurrentDay(ZonedDateTime time) {
        return (int) time.toLocalDate().toEpochDay();
    }

    /**
     * Retourne la prochaine heure de reset (demain 6h si on a dépassé 6h aujourd'hui).
     */
    public ZonedDateTime getNextResetTime() {
        ZonedDateTime now = ZonedDateTime.now(serverTimezone);
        ZonedDateTime nextReset = now.withHour(RESET_HOUR).withMinute(0).withSecond(0).withNano(0);

        // Si on a dépassé 6h aujourd'hui, le prochain reset est demain
        if (now.getHour() >= RESET_HOUR) {
            nextReset = nextReset.plusDays(1);
        }

        return nextReset;
    }

    /**
     * Retourne le temps restant avant le prochain reset.
     */
    public Duration getTimeUntilNextReset() {
        ZonedDateTime now = ZonedDateTime.now(serverTimezone);
        return Duration.between(now, getNextResetTime());
    }

    /**
     * Vérifie si on a déjà reset aujourd'hui.
     */
    public boolean hasResetToday() {
        ZonedDateTime now = ZonedDateTime.now(serverTimezone);
        return lastResetDay == getCurrentDay(now);
    }

    // ============================================================================
    // PERSISTENCE - SAUVEGARDE/CHARGEMENT DB
    // ============================================================================

    /**
     * Charge le dernier reset depuis la DB.
     * Appelé au démarrage du TimeManager.
     */
    private void loadLastReset() {
        try {
            String data = GameManager.getInstance().getDatabase().getServerData("last_daily_reset");

            if (data != null && !data.isEmpty()) {
                String[] parts = data.split(":");
                this.lastResetTimestamp = Long.parseLong(parts[0]);
                this.lastResetDay = Integer.parseInt(parts[1]);

                Instant instant = Instant.ofEpochMilli(lastResetTimestamp);
                ZonedDateTime resetTime = ZonedDateTime.ofInstant(instant, serverTimezone);

                logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"Dernier reset chargé depuis DB :");
                logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"  ├─ Jour : " + lastResetDay);
                logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"  └─ Date : " + resetTime.format(
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
            } else {
                // Premier démarrage - pas de reset enregistré
                ZonedDateTime now = ZonedDateTime.now(serverTimezone);
                this.lastResetTimestamp = now.toInstant().toEpochMilli();
                this.lastResetDay = getCurrentDay(now);
                saveLastReset();

                logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"Premier démarrage du TimeManager");
                logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"  └─ Initialisation du système de reset");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, LogManager.ETagLog.SYSTEM,"╔════════════════════════════════════╗");
            logger.log(Level.SEVERE, LogManager.ETagLog.SYSTEM,"║ ERREUR CHARGEMENT RESET !          ║");
            logger.log(Level.SEVERE, LogManager.ETagLog.SYSTEM,"╠════════════════════════════════════╣");
            logger.log(Level.SEVERE, LogManager.ETagLog.SYSTEM,"║ " + e.getMessage());
            logger.log(Level.SEVERE, LogManager.ETagLog.SYSTEM,"║ → Fallback : force un reset        ║");
            logger.log(Level.SEVERE, LogManager.ETagLog.SYSTEM,"╚════════════════════════════════════╝");

            // Fallback : force un reset aujourd'hui
            ZonedDateTime now = ZonedDateTime.now(serverTimezone);
            this.lastResetDay = getCurrentDay(now) - 1;  // Hier pour forcer reset
        }
    }

    /**
     * Sauvegarde le dernier reset en DB.
     * Format : "timestamp:daysSinceEpoch"
     * Exemple : "1708502400000:19750"
     */
    private void saveLastReset() {
        try {
            String data = lastResetTimestamp + ":" + lastResetDay;
            GameManager.getInstance().getDatabase().saveServerData("last_daily_reset", data);
        } catch (Exception e) {
            logger.log(Level.SEVERE, LogManager.ETagLog.SYSTEM,"ERREUR lors de la sauvegarde du reset : " + e.getMessage());
            e.printStackTrace();
        }
    }
}