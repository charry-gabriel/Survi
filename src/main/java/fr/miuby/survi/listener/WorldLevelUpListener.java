package fr.miuby.survi.listener;

import fr.miuby.survi.sound.ESound;
import fr.miuby.survi.sound.SoundService;
import fr.miuby.survi.world.event.WorldLevelUpEvent;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.lang.LangKey;
import fr.miuby.survi.system.lang.LangService;
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

    private static final Title.Times TITLE_TIMES = Title.Times.times(
            Duration.ofMillis(500),
            Duration.ofMillis(3000),
            Duration.ofMillis(700)
    );

    @EventHandler
    public void onWorldLevelUp(WorldLevelUpEvent event) {
        LangService ls = GameManager.getInstance().getLangService();
        int oldLevel = event.getOldLevel();
        int newLevel = event.getNewLevel();

        for (Player p : Bukkit.getOnlinePlayers()) {
            SoundService.play(p, ESound.WORLD_LEVEL_UP);
            p.showTitle(Title.title(
                    ls.text(p, LangKey.WORLD_LEVEL_UP_TITLE, newLevel),
                    ls.text(p, LangKey.WORLD_LEVEL_UP_SUBTITLE),
                    TITLE_TIMES
            ));
            p.sendMessage(ls.text(p, LangKey.WORLD_LEVEL_UP_BROADCAST, oldLevel, newLevel));
        }
    }
}