package fr.miuby.survi;

import fr.miuby.survi.system.YmlResourceManager;
import fr.miuby.survi.system.database.SqlCommand;
import fr.miuby.survi.item.CustomItemCommand;
import fr.miuby.survi.listener.*;
import fr.miuby.survi.job.JobCommand;
import fr.miuby.survi.quest.QuestCommand;
import fr.miuby.survi.quest.ReputationCommand;
import fr.miuby.survi.quest.QuestListener;
import fr.miuby.survi.role.RoleCommand;
import fr.miuby.survi.system.command.SystemCommand;
import fr.miuby.survi.system.time.TimeManager;
import fr.miuby.survi.villager.VillagerCommand;
import fr.miuby.survi.villager.blessing.BlessingCommand;
import fr.miuby.survi.world.WorldCommand;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Survi extends JavaPlugin {
    @Override
    public void onEnable() {
        updateResources();

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
        pluginManager.registerEvents(new QuestListener(), this);
        pluginManager.registerEvents(new GraveListener(), this);

        getConfig().options().copyDefaults(true);
        saveConfig();

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(SqlCommand.createCommand().build());
            commands.registrar().register(VillagerCommand.createCommand().build());
            commands.registrar().register(CustomItemCommand.createCommand().build());
            commands.registrar().register(RoleCommand.createRoleCommand().build());
            commands.registrar().register(RoleCommand.createSubRoleCommand().build());
            commands.registrar().register(QuestCommand.createCommand().build());
            commands.registrar().register(JobCommand.createCommand().build());
            commands.registrar().register(ReputationCommand.createCommand().build());
            commands.registrar().register(SystemCommand.createCommand().build());
            commands.registrar().register(BlessingCommand.createCommand().build());
            commands.registrar().register(WorldCommand.createWorldResetCommand().build());
            commands.registrar().register(WorldCommand.createTeleportToCommand().build());
        });

        GameManager.getInstance().init(this);
    }

    @Override
    public void onDisable() {
        TimeManager tm = GameManager.getInstance().getTimeManager();
        if (tm != null) {
            tm.stop();
        }
    }

    private void updateResources() {
        YmlResourceManager.update(this, "quests.yml");
        YmlResourceManager.update(this, "recipes.yml");

        String[] villagerFiles = getVillagerResourcePaths();
        for (String path : villagerFiles) {
            YmlResourceManager.update(this, path);
        }
    }

    private String[] getVillagerResourcePaths() {
        return new String[]{
                "villagers/survivant.yml",
                "villagers/nain.yml",
                "villagers/maddox.yml",
                "villagers/thomas.yml",
                "villagers/francois.yml",
        };
    }
}