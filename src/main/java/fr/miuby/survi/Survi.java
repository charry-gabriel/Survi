package fr.miuby.survi;

import fr.miuby.lib.resource.MLResourceManager;
import fr.miuby.survi.display.TabSkins;
import fr.miuby.survi.job.ReputationCommand;
import fr.miuby.survi.mob.MobCommand;
import fr.miuby.survi.system.database.SqlCommand;
import fr.miuby.survi.item.CustomItemCommand;
import fr.miuby.survi.listener.*;
import fr.miuby.survi.quest.GlobalQuestCommand;
import fr.miuby.survi.quest.QuestCommand;
import fr.miuby.survi.quest.QuestListener;
import fr.miuby.survi.role.RoleCommand;
import fr.miuby.survi.system.command.SystemCommand;
import fr.miuby.survi.system.time.TimeManager;
import fr.miuby.survi.villager.VillagerCommand;
import fr.miuby.survi.blessing.BlessingCommand;
import fr.miuby.survi.world.WorldCommand;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Survi extends JavaPlugin {
    @Override
    public void onEnable() {
        updateResources();

        TabSkins.load(this);

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
        pluginManager.registerEvents(new JobListener(), this);
        pluginManager.registerEvents(new MobSpawnListener(), this);

        getConfig().options().copyDefaults(true);
        saveConfig();

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(SqlCommand.createCommand().build());
            commands.registrar().register(VillagerCommand.createCommand().build());
            commands.registrar().register(CustomItemCommand.createCommand().build());
            commands.registrar().register(RoleCommand.createRoleCommand().build());
            commands.registrar().register(RoleCommand.createSubRoleCommand().build());
            commands.registrar().register(QuestCommand.createCommand().build());
            commands.registrar().register(GlobalQuestCommand.createCommand().build());
            commands.registrar().register(ReputationCommand.createReputationCommand().build());
            commands.registrar().register(SystemCommand.createCommand().build());
            commands.registrar().register(BlessingCommand.createCommand().build());
            commands.registrar().register(WorldCommand.createWorldResetCommand().build());
            commands.registrar().register(WorldCommand.createTeleportToCommand().build());
            commands.registrar().register(MobCommand.createCommand().build());
        });

        GameManager.getInstance().init(this);
    }

    @Override
    public void onDisable() {
        TimeManager tm = GameManager.getInstance().getTimeManager();
        if (tm != null) {
            tm.stop();
        }
        GameManager.getInstance().getVillageZoneManager().stop();
    }

    private void updateResources() {
        MLResourceManager.deployFolder(this, "villagers");
        MLResourceManager.deployFolder(this, "traders");
        MLResourceManager.deploy(this, "config.yml");
        MLResourceManager.deploy(this, "quests.yml");
        MLResourceManager.deploy(this, "global_quests.yml");
        MLResourceManager.deploy(this, "recipes.yml");
        MLResourceManager.deploy(this, "monsters.yml");
        MLResourceManager.deploy(this, "roles.yml");
    }
}