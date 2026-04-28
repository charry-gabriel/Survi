package fr.miuby.survi.system.time;

import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.SurviConfig;
import fr.miuby.survi.system.log.LogManager;
import fr.miuby.survi.system.time.event.DailyResetEvent;
import fr.miuby.survi.world.EWorld;
import lombok.Getter;
import org.bukkit.scheduler.BukkitTask;

import java.time.*;
import java.util.logging.Level;

public class TimeManager {

    private static final int SYNC_INTERVAL_TICKS = 20;
    private static final int RESET_CHECK_INTERVAL_TICKS = 20 * 60;

    private static final int MC_FULL_DAY = 24000;

    private final LogManager logger = LogManager.getInstance();
    @Getter
    private final ZoneId serverTimezone;

    private BukkitTask syncTask;
    private BukkitTask resetCheckTask;

    @Getter
    private long lastResetTimestamp;
    @Getter
    private int lastResetDay;

    public TimeManager() {
        this.serverTimezone = ZoneId.systemDefault();
        loadLastReset();
    }

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
        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM, "  ├─ Reset quotidien: " + SurviConfig.getInstance().getDailyResetHour() + "h00");
        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM, "  └─ Prochain reset: " + getNextResetTime().format(
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
    }

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

    //region real time
    private void updateMinecraftTime() {
        ZonedDateTime now = ZonedDateTime.now(serverTimezone);

        int hour = now.getHour();
        int minute = now.getMinute();

        double currentTime = hour + (minute / 60.0);

        int ticks = calculateTicks(currentTime);
        boolean isNight = checkIfNight(currentTime);

        // Applique au monde Village
        WorldRegistry.get(EWorld.VILLAGE).getWorld().setTime(ticks);
        GameManager.getInstance().setNight(isNight);
    }

    private int calculateTicks(double currentTime) {
        // Calcule les durées
        double dayLength = getDayLength();
        double nightLength = 24.0 - dayLength;

        // Vérifie si on est le jour ou la nuit
        if (isDayTime(currentTime)) {
            // === JOUR ===
            // Calcule combien d'heures depuis le lever
            double hoursSinceSunrise;
            if (SurviConfig.getInstance().getSunsetHour() == 0 || SurviConfig.getInstance().getSunsetHour() < SurviConfig.getInstance().getSunriseHour()) {
                // Cas wrap (ex: lever 9h, coucher 0h OU lever 20h, coucher 6h)
                if (currentTime >= SurviConfig.getInstance().getSunriseHour()) {
                    hoursSinceSunrise = currentTime - SurviConfig.getInstance().getSunriseHour();
                } else {
                    hoursSinceSunrise = (24 - SurviConfig.getInstance().getSunriseHour()) + currentTime;
                }
            } else {
                // Cas normal (ex: lever 6h, coucher 18h)
                hoursSinceSunrise = currentTime - SurviConfig.getInstance().getSunriseHour();
            }

            // Progression du jour (0.0 → 1.0)
            double progress = hoursSinceSunrise / dayLength;

            // Ticks du jour : 0 → 12000
            return (int) (progress * 12000);

        } else {
            // === NUIT ===
            // Calcule combien d'heures depuis le coucher
            double hoursSinceSunset;
            if (SurviConfig.getInstance().getSunsetHour() == 0) {
                // Coucher à minuit : 0h = début de nuit
                hoursSinceSunset = currentTime;
            } else if (SurviConfig.getInstance().getSunriseHour() > SurviConfig.getInstance().getSunsetHour()) {
                // Cas wrap
                if (currentTime >= SurviConfig.getInstance().getSunsetHour() && currentTime < SurviConfig.getInstance().getSunriseHour()) {
                    hoursSinceSunset = currentTime - SurviConfig.getInstance().getSunsetHour();
                } else if (currentTime >= SurviConfig.getInstance().getSunsetHour()) {
                    hoursSinceSunset = currentTime - SurviConfig.getInstance().getSunsetHour();
                } else {
                    hoursSinceSunset = (24 - SurviConfig.getInstance().getSunsetHour()) + currentTime;
                }
            } else {
                // Cas normal
                if (currentTime >= SurviConfig.getInstance().getSunsetHour()) {
                    hoursSinceSunset = currentTime - SurviConfig.getInstance().getSunsetHour();
                } else {
                    hoursSinceSunset = (24 - SurviConfig.getInstance().getSunsetHour()) + currentTime;
                }
            }

            double progress = hoursSinceSunset / nightLength;

            int nightTicks = 12000 + (int) (progress * 12000);
            return nightTicks % 24000;
        }
    }

    private boolean isDayTime(double currentTime) {
        if (SurviConfig.getInstance().getSunsetHour() == 0) {
            // Coucher à minuit : jour si >= lever
            return currentTime >= SurviConfig.getInstance().getSunriseHour();
        } else if (SurviConfig.getInstance().getSunsetHour() > SurviConfig.getInstance().getSunriseHour()) {
            // Cas normal : lever < coucher
            return currentTime >= SurviConfig.getInstance().getSunriseHour() && currentTime < SurviConfig.getInstance().getSunsetHour();
        } else {
            // Cas wrap : lever > coucher
            return currentTime >= SurviConfig.getInstance().getSunriseHour() || currentTime < SurviConfig.getInstance().getSunsetHour();
        }
    }

    private boolean checkIfNight(double currentTime) {
        return !isDayTime(currentTime);
    }

    private double getDayLength() {
        if (SurviConfig.getInstance().getSunsetHour() == 0) {
            // Ex: lever 9h, coucher 0h → 24 - 9 = 15h
            return 24.0 - SurviConfig.getInstance().getSunriseHour();
        } else if (SurviConfig.getInstance().getSunsetHour() > SurviConfig.getInstance().getSunriseHour()) {
            // Ex: lever 6h, coucher 18h → 18 - 6 = 12h
            return SurviConfig.getInstance().getSunsetHour() - SurviConfig.getInstance().getSunriseHour();
        } else {
            // Ex: lever 20h, coucher 6h → (24-20) + 6 = 10h
            return (24.0 - SurviConfig.getInstance().getSunriseHour()) + SurviConfig.getInstance().getSunsetHour();
        }
    }
    //endregion

    //region daily reset
    private void checkForReset() {
        ZonedDateTime now = ZonedDateTime.now(serverTimezone);
        int currentDay = getCurrentDay(now);

        if (currentDay > lastResetDay && now.getHour() >= SurviConfig.getInstance().getDailyResetHour()) {
            performDailyReset(now);
        }
    }

    private void checkForMissedReset() {
        ZonedDateTime now = ZonedDateTime.now(serverTimezone);
        int currentDay = getCurrentDay(now);

        if (currentDay > lastResetDay && now.getHour() >= SurviConfig.getInstance().getDailyResetHour()) {
            logger.log(Level.WARNING, LogManager.ETagLog.SYSTEM,"╔════════════════════════════════════╗");
            logger.log(Level.WARNING, LogManager.ETagLog.SYSTEM,"║   RESET MANQUÉ DÉTECTÉ !           ");
            logger.log(Level.WARNING, LogManager.ETagLog.SYSTEM,"╠════════════════════════════════════╣");
            logger.log(Level.WARNING, LogManager.ETagLog.SYSTEM,"║ Dernier reset : jour " + lastResetDay);
            logger.log(Level.WARNING, LogManager.ETagLog.SYSTEM,"║ Jour actuel   : jour " + currentDay);
            logger.log(Level.WARNING, LogManager.ETagLog.SYSTEM,"║ → Reset immédiat déclenché         ");
            logger.log(Level.WARNING, LogManager.ETagLog.SYSTEM,"╚════════════════════════════════════╝");
            performDailyReset(now);
        } else {
            logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"Aucun reset manqué (dernier reset : jour " + lastResetDay + ")");
        }
    }

    private void performDailyReset(ZonedDateTime now) {
        int currentDay = getCurrentDay(now);
        long timestamp = now.toInstant().toEpochMilli();

        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"╔═══════════════════════════════════════════════╗");
        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"║         RESET QUOTIDIEN - 6H00                ");
        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"╠═══════════════════════════════════════════════╣");
        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"║ Jour : " + currentDay + " (précédent : " + lastResetDay + ")");
        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"║ Heure : " + now.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm:ss")));
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

    public void forceReset() {
        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"╔═══════════════════════════════════════════════╗");
        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"║      RESET FORCÉ MANUELLEMENT                 ");
        logger.log(Level.INFO, LogManager.ETagLog.SYSTEM,"╚═══════════════════════════════════════════════╝");
        performDailyReset(ZonedDateTime.now(serverTimezone));
    }
    //endregion

    private int getCurrentDay(ZonedDateTime time) {
        return (int) time.toLocalDate().toEpochDay();
    }

    public ZonedDateTime getNextResetTime() {
        ZonedDateTime now = ZonedDateTime.now(serverTimezone);
        ZonedDateTime nextReset = now.withHour(SurviConfig.getInstance().getDailyResetHour()).withMinute(0).withSecond(0).withNano(0);

        // Si on a dépassé 6h aujourd'hui, le prochain reset est demain
        if (now.getHour() >= SurviConfig.getInstance().getDailyResetHour()) {
            nextReset = nextReset.plusDays(1);
        }

        return nextReset;
    }

    public boolean hasResetToday() {
        ZonedDateTime now = ZonedDateTime.now(serverTimezone);
        return lastResetDay == getCurrentDay(now);
    }

    public static String formatTime(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        if (days > 0) return days + " jours et " + hours + "h" + minutes;
        if (hours > 0) return hours + "h" + minutes;
        if (minutes > 0) return minutes + "m";
        return "moins d'1 minute";
    }

    //region save/load
    private void loadLastReset() {
        try {
            String data = GameManager.getInstance().getDatabase().system().getServerData("last_daily_reset");

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

    private void saveLastReset() {
        try {
            String data = lastResetTimestamp + ":" + lastResetDay;
            GameManager.getInstance().getDatabase().system().saveServerData("last_daily_reset", data);
        } catch (Exception e) {
            logger.log(Level.SEVERE, LogManager.ETagLog.SYSTEM,"ERREUR lors de la sauvegarde du reset : " + e.getMessage());
        }
    }
    //endregion
}