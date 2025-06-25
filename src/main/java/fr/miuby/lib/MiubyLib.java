package fr.miuby.lib;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class MiubyLib {
    private static JavaPlugin plugin;

    public static void init(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
    }

    public static void runLater(Runnable task, long delay) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delay);
    }

    public static Logger getLogger() {
        return plugin.getLogger();
    }
}
