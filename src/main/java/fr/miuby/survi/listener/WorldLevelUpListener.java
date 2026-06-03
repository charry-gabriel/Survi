package fr.miuby.survi.listener;

import fr.miuby.survi.world.event.WorldLevelUpEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.Duration;

/**
 * Réagit à {@link WorldLevelUpEvent} sans polluer {@code WorldLevelManager} :
 *
 * <ul>
 *   <li><b>Title</b>    — affiché à tous les joueurs en ligne</li>
 *   <li><b>Son</b>      — joué pour tous les joueurs</li>
 *   <li><b>Broadcast</b>— annonce globale en chat</li>
 * </ul>
 */
public class WorldLevelUpListener implements Listener {

    private static final Sound WORLD_LEVEL_SOUND = Sound.sound(Key.key("ui.toast.challenge_complete"), Sound.Source.MASTER, 1f, 0.8f);

    private static final Title.Times TITLE_TIMES = Title.Times.times(
            Duration.ofMillis(500),
            Duration.ofMillis(3000),
            Duration.ofMillis(700)
    );

    @EventHandler
    public void onWorldLevelUp(WorldLevelUpEvent event) {
        Component broadcast = Component.text("✦ Le monde a évolué ! ", NamedTextColor.GOLD)
                .append(Component.text("Niveau " + event.getOldLevel(), NamedTextColor.YELLOW))
                .append(Component.text(" → ", NamedTextColor.GRAY))
                .append(Component.text("Niveau " + event.getNewLevel(), NamedTextColor.GOLD));

        Title title = Title.title(
                Component.text("✦ Monde — Niveau " + event.getNewLevel(), NamedTextColor.GOLD),
                Component.text("Le monde devient plus difficile…", NamedTextColor.YELLOW),
                TITLE_TIMES
        );

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(WORLD_LEVEL_SOUND);
            p.showTitle(title);
        }
        Bukkit.broadcast(broadcast);
    }
}