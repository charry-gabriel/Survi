package fr.miuby.survi.listener;

import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.WorldFactory;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class WorldListener implements Listener {
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        WorldFactory.initializeIfNeeded();
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        if (event.getReason().equals(PortalCreateEvent.CreateReason.FIRE)) {
            if (WorldRegistry.get(EWorld.NETHER).isLocked())
                event.setCancelled(true);
        } else if (event.getReason().equals(PortalCreateEvent.CreateReason.END_PLATFORM)) {
            if (WorldRegistry.get(EWorld.END).isLocked())
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        AlphaPlayer alphaPlayer = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (alphaPlayer == null)
            return;

        alphaPlayer.setWorld(WorldRegistry.get(event.getPlayer().getWorld().getUID()));
        GameManager.getInstance().getAlphaPlayerFactory().sendToPlayers(alphaPlayer);
        GameManager.getInstance().getPlayerAttributeService().reapplyAllRoleAttributes(alphaPlayer);
    }
}
