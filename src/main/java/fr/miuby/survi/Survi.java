package fr.miuby.survi;

import fr.miuby.survi.database.SqlCommand;
import fr.miuby.survi.listener.*;
import fr.miuby.survi.role.RoleTabCompleter;
import fr.miuby.survi.role.SubRoleTabCompleter;
import fr.miuby.survi.villager.VillagerCommand;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public class Survi extends JavaPlugin {
    private final Commands commands = new Commands();

    @Override
    public void onEnable() {
        PluginManager pluginManager = this.getServer().getPluginManager();
        pluginManager.registerEvents(new ServerListener(), this);
        pluginManager.registerEvents(new PlayerListener(), this);
        pluginManager.registerEvents(new DamageListener(), this);
        pluginManager.registerEvents(new WorldListener(), this);
        pluginManager.registerEvents(new ItemListener(), this);
        pluginManager.registerEvents(new AlphaPlayerListener(), this);
        pluginManager.registerEvents(new GrowthItemListener(), this);

        getConfig().options().copyDefaults(true);
        saveConfig();

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(SqlCommand.createCommand().build());
            commands.registrar().register(VillagerCommand.createCommand().build());
        });

        Objects.requireNonNull(getCommand("role")).setTabCompleter(new RoleTabCompleter());
        Objects.requireNonNull(getCommand("subrole")).setTabCompleter(new SubRoleTabCompleter());

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