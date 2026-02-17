package fr.miuby.survi;

import fr.miuby.survi.database.SqlCommand;
import fr.miuby.survi.item.CustomItemCommand;
import fr.miuby.survi.listener.*;
import fr.miuby.survi.role.RoleCommand;
import fr.miuby.survi.villager.VillagerCommand;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Survi extends JavaPlugin {
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
        pluginManager.registerEvents(new CropGrowthListener(), this);
        pluginManager.registerEvents(new VillagerListener(), this);

        getConfig().options().copyDefaults(true);
        saveConfig();

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(SqlCommand.createCommand().build());
            commands.registrar().register(VillagerCommand.createCommand().build());
            commands.registrar().register(CustomItemCommand.createCommand().build());
            commands.registrar().register(RoleCommand.createRoleCommand().build());
            commands.registrar().register(RoleCommand.createSubRoleCommand().build());
        });

        GameManager.getInstance().init(this);
    }
}