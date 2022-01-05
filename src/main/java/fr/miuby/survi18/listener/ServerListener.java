package fr.miuby.survi18.listener;

import fr.miuby.survi18.Survi18;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.PortalCreateEvent;

public class ServerListener  implements Listener {
    static Survi18 plugin;

    public ServerListener(Survi18 instance) {
        plugin = instance;
    }
}
