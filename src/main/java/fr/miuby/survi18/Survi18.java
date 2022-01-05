package fr.miuby.survi18;

import fr.miuby.survi18.listener.MyListener;
import fr.miuby.survi18.listener.PlayerListener;
import fr.miuby.survi18.listener.ServerListener;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Objective;

public class Survi18 extends JavaPlugin {
    private final MyListener listener = new MyListener(this);
    private final PlayerListener playerListener = new PlayerListener(this);
    private final ServerListener serverListener = new ServerListener(this);
    private final Commands commands = new Commands(this);

    @Override
    public void onEnable() {
        PluginManager pluginManager = this.getServer().getPluginManager();
        pluginManager.registerEvents(listener, this);
        pluginManager.registerEvents(playerListener, this);
        pluginManager.registerEvents(serverListener, this);

        getConfig().options().copyDefaults(true);
        saveConfig();

        GameManager.getInstance().Init(this);
    }

    @Override
    public void onDisable() {
        GameManager.getInstance().getVillage().DeleteVillagers();
        GameManager.getInstance().getDatabaseManager().close();
    }

    @Override
    public boolean onCommand(CommandSender senderC, Command cmd, String commandLabel, String[] args) {
        if(senderC instanceof Player) {
            Player sender = (Player) senderC;
            String commandName = cmd.getName().toLowerCase();

            return commands.doCommand(sender, commandName, args);
        }
        return false;
    }
}
