package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.database.Errors;
import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.Objects;
import java.util.logging.Level;

public class ServerListener implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        GameManager.getInstance().getAlphaPlayerFactory().playerJoin(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        AlphaPlayer.get(event.getPlayer().getUniqueId()).resetPlayer();
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event){
        if (event.getEntity() instanceof EnderDragon dragon) {
            try {
                Objects.requireNonNull(dragon.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(2000);
                dragon.setHealth(2000);
            } catch (Exception exception) {
                GameManager.getInstance().getLogger().log(Level.SEVERE, Errors.nullException, exception);
            }
        }
    }
}
