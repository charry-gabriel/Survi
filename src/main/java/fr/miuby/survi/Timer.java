package fr.miuby.survi;

import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.Monde;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

public class Timer {

    public void update() {
        GameManager.getInstance().getScheduler().scheduleSyncRepeatingTask(GameManager.getInstance().getPlugin(), () -> {

            ZonedDateTime nowZoned = ZonedDateTime.now();
            Instant midnight = nowZoned.toLocalDate().atStartOfDay(nowZoned.getZone()).toInstant();
            Duration duration = Duration.between(midnight, Instant.now());
            long seconds = duration.getSeconds();
            int tick;
            if (seconds >= 0 && seconds < 28800) {
                tick = 12000 + Math.round(seconds/1.8f);
                GameManager.getInstance().setNight(false);
            }
            else {
                tick = Math.round(seconds/9.6f);
                GameManager.getInstance().setNight(true);
            }
            Monde.get(EWorld.VILLAGE).getWorld().setTime(tick);
        }, 0, 20);
    }
}
