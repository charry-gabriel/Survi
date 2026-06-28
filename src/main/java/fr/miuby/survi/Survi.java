package fr.miuby.survi;

import fr.miuby.lib.resource.MLResourceManager;
import fr.miuby.survi.display.TabSkins;
import fr.miuby.survi.job.JobCommand;
import fr.miuby.survi.job.rare.RareJobItemCommand;
import fr.miuby.survi.listener.job.*;
import fr.miuby.survi.mob.MobCommand;
import fr.miuby.survi.system.database.SqlCommand;
import fr.miuby.survi.item.CustomItemCommand;
import fr.miuby.survi.item.growth_item.GrowthItemCommand;
import fr.miuby.survi.job.task.FishermanEffectsTask;
import fr.miuby.survi.job.task.MinerEffectsTask;
import fr.miuby.survi.world.zone.ZoneBorderTask;
import fr.miuby.survi.listener.*;
import fr.miuby.survi.quest.globalquest.GlobalQuestCommand;
import fr.miuby.survi.quest.quest.QuestCommand;
import fr.miuby.survi.listener.QuestListener;
import fr.miuby.survi.role.RoleCommand;
import fr.miuby.survi.system.command.SystemCommand;
import fr.miuby.survi.system.command.PlayerCommand;
import fr.miuby.survi.system.time.TimeManager;
import fr.miuby.survi.villager.VillagerCommand;
import fr.miuby.survi.blessing.BlessingCommand;
import fr.miuby.survi.world.RainManager;
import fr.miuby.survi.world.WorldCommand;
import fr.miuby.survi.world.PortalLocatorManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Survi extends JavaPlugin {
    @Override
    public void onEnable() {
        updateResources();

        TabSkins.load(this);

        PluginManager pluginManager = this.getServer().getPluginManager();

        PlacedBlockTracker placedBlockTracker = new PlacedBlockTracker();

        pluginManager.registerEvents(new ServerListener(), this);
        pluginManager.registerEvents(new PlayerListener(), this);
        pluginManager.registerEvents(new DamageListener(), this);
        pluginManager.registerEvents(new WorldListener(), this);
        pluginManager.registerEvents(new ItemListener(), this);
        pluginManager.registerEvents(new AlphaPlayerListener(), this);
        pluginManager.registerEvents(new CropGrowthListener(), this);
        pluginManager.registerEvents(new VillagerListener(), this);
        pluginManager.registerEvents(new GraveListener(), this);
        pluginManager.registerEvents(new MinerListener(), this);
        pluginManager.registerEvents(new LumberjackListener(placedBlockTracker), this);
        pluginManager.registerEvents(new FarmerListener(), this);
        pluginManager.registerEvents(new EnchanterListener(), this);
        pluginManager.registerEvents(new JobLevelUpListener(), this);
        pluginManager.registerEvents(new WorldLevelUpListener(), this);
        pluginManager.registerEvents(new VillageZoneStageUpListener(), this);
        pluginManager.registerEvents(new MobSpawnListener(), this);
        pluginManager.registerEvents(new GrowthItemListener(placedBlockTracker), this);
        pluginManager.registerEvents(new QuestListener(placedBlockTracker), this);
        pluginManager.registerEvents(new OfflineNotificationListener(), this);
        pluginManager.registerEvents(new FishermanListener(), this);
        pluginManager.registerEvents(new AlchemicPotionListener(), this);
        pluginManager.registerEvents(new FoodListener(), this);
        pluginManager.registerEvents(new RareJobItemListener(placedBlockTracker), this);

        pluginManager.registerEvents(placedBlockTracker, this);

        new FishermanEffectsTask().runTaskTimer(this, 20L, FishermanEffectsTask.PERIOD_TICKS);
        new ZoneBorderTask().runTaskTimer(this, 20L, ZoneBorderTask.PERIOD_TICKS);
        new MinerEffectsTask().runTaskTimer(this, 20L, MinerEffectsTask.PERIOD_TICKS);

        getConfig().options().copyDefaults(true);
        saveConfig();

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(SqlCommand.createCommand().build());
            commands.registrar().register(VillagerCommand.createCommand().build());
            commands.registrar().register(CustomItemCommand.createCommand().build());
            commands.registrar().register(GrowthItemCommand.createCommand().build());
            commands.registrar().register(RoleCommand.createRoleCommand().build());
            commands.registrar().register(RoleCommand.createSubRoleCommand().build());
            commands.registrar().register(QuestCommand.createCommand().build());
            commands.registrar().register(GlobalQuestCommand.createCommand().build());
            commands.registrar().register(JobCommand.createReputationCommand().build());
            commands.registrar().register(RareJobItemCommand.createCommand().build());
            commands.registrar().register(SystemCommand.createCommand().build());
            commands.registrar().register(BlessingCommand.createCommand().build());
            commands.registrar().register(WorldCommand.createWorldResetCommand().build());
            commands.registrar().register(WorldCommand.createTeleportToCommand().build());
            commands.registrar().register(MobCommand.createCommand().build());
            commands.registrar().register(PlayerCommand.createCommand().build());
        });

        GameManager.getInstance().init(this);
    }

    @Override
    public void onDisable() {
        TimeManager tm = GameManager.getInstance().getTimeManager();
        if (tm != null) tm.stop();

        PortalLocatorManager plm = GameManager.getInstance().getPortalLocatorManager();
        if (plm != null) plm.stop();

        RainManager rm = GameManager.getInstance().getRainManager();
        if (rm != null) rm.stop();
    }

    private void updateResources() {
        MLResourceManager.deployFolder(this, "villagers");
        MLResourceManager.deployFolder(this, "traders");
        MLResourceManager.deployFolder(this, "growth_items");
        MLResourceManager.deployFolder(this, "quests");
        MLResourceManager.deployFolder(this, "growth_items");
        MLResourceManager.deployFolder(this, "jobs");
        MLResourceManager.deployFolder(this, "lang");
        MLResourceManager.deploy(this, "config.yml");
        MLResourceManager.deploy(this, "zone.yml");
        MLResourceManager.deploy(this, "global_quests.yml");
        MLResourceManager.deploy(this, "recipes.yml");
        MLResourceManager.deploy(this, "monsters.yml");
        MLResourceManager.deploy(this, "roles.yml");
        MLResourceManager.deploy(this, "rare_items.yml");
    }
}