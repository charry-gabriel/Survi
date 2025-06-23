package fr.miuby.survi;

import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.WorldFactory;

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
                tick = Math.round( (6000f / 14400f) * seconds + 12000);
                GameManager.getInstance().setNight(false);
            }
            else {
                tick = Math.round( (6000f / 28800f) * seconds - 6000);
                GameManager.getInstance().setNight(true);
            }
            WorldFactory.get(EWorld.VILLAGE).getWorld().setTime(tick);
        }, 0, 20);
    }
}