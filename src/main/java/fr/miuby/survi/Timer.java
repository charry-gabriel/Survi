package fr.miuby.survi;

import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.Monde;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

public class Timer {

    public void update() {
        BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(GameManager.getInstance().getPlugin(), () -> {

            ZonedDateTime nowZoned = ZonedDateTime.now();
            Instant midnight = nowZoned.toLocalDate().atStartOfDay(nowZoned.getZone()).toInstant();
            Duration duration = Duration.between(midnight, Instant.now());
            long seconds = duration.getSeconds();
            int tick;
            if (seconds >= 0 && seconds < 28800) {
                tick = 12000 + Math.round(seconds/1.8f);
            }
            else {
                tick = Math.round(seconds/9.6f);
            }
            Monde.get(EWorld.VILLAGE).getWorld().setTime(tick);
        }, 0, 20);
    }
}
