package fr.miuby.survi.listener;

import org.bukkit.event.Listener;

public class ServerListener  implements Listener {
    static fr.miuby.survi.Survi plugin;

    public ServerListener(fr.miuby.survi.Survi instance) {
        plugin = instance;
    }
}
