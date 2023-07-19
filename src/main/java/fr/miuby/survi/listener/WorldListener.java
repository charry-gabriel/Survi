package fr.miuby.survi.listener;

import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.Monde;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.PortalCreateEvent;

public class WorldListener implements Listener {
    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        if (event.getReason().equals(PortalCreateEvent.CreateReason.FIRE)) {
            if (!Monde.get(EWorld.NETHER).isLocked())
                event.setCancelled(true);
        } else {
            if (!Monde.get(EWorld.END).isLocked())
                event.setCancelled(true);
        }
    }
}
