package fr.miuby.survi;

import fr.miuby.survi.listener.MyListener;
import fr.miuby.survi.listener.PlayerListener;
import fr.miuby.survi.listener.ServerListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class Survi extends JavaPlugin {
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
