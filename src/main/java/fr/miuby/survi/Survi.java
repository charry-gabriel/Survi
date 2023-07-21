package fr.miuby.survi;

import fr.miuby.survi.listener.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class Survi extends JavaPlugin {
    private final Commands commands = new Commands(this);

    @Override
    public void onEnable() {
        PluginManager pluginManager = this.getServer().getPluginManager();
        pluginManager.registerEvents(new ServerListener(), this);
        pluginManager.registerEvents(new PlayerListener(), this);
        pluginManager.registerEvents(new DamageListener(), this);
        pluginManager.registerEvents(new WorldListener(), this);
        pluginManager.registerEvents(new ItemListener(), this);

        getConfig().options().copyDefaults(true);
        saveConfig();

        GameManager.getInstance().init(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender senderC, @NotNull Command cmd, @NotNull String commandLabel, String[] args) {
        if(senderC instanceof Player) {
            Player sender = (Player) senderC;
            String commandName = cmd.getName().toLowerCase();

            return commands.doCommand(sender, commandName, args);
        }
        return false;
    }
}
