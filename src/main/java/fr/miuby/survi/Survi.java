package fr.miuby.survi;

import fr.miuby.survi.job.ReputationCommand;
import fr.miuby.survi.mob.MobCommand;
import fr.miuby.survi.system.YmlResourceManager;
import fr.miuby.survi.system.database.SqlCommand;
import fr.miuby.survi.item.CustomItemCommand;
import fr.miuby.survi.listener.*;
import fr.miuby.survi.quest.GlobalQuestCommand;
import fr.miuby.survi.quest.QuestCommand;
import fr.miuby.survi.quest.QuestListener;
import fr.miuby.survi.role.RoleCommand;
import fr.miuby.survi.system.command.SystemCommand;
import fr.miuby.survi.system.log.LogManager;
import fr.miuby.survi.system.time.TimeManager;
import fr.miuby.survi.villager.VillagerCommand;
import fr.miuby.survi.villager.villagerlevel.blessing.BlessingCommand;
import fr.miuby.survi.world.WorldCommand;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

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
        updateFolderResources("villagers");
        updateFolderResources("traders");

        YmlResourceManager.update(this, "config.yml");
        YmlResourceManager.update(this, "quests.yml");
        YmlResourceManager.update(this, "global_quests.yml");
        YmlResourceManager.update(this, "recipes.yml");
        YmlResourceManager.update(this, "monsters.yml");
        YmlResourceManager.update(this, "roles.yml");
    }

    private void updateFolderResources(String folder) {
        try {
            File file = getFile();

            try (JarFile jar = new JarFile(file)) {
                Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();

                    String name = entry.getName();

                    if (name.startsWith(folder + "/")
                            && name.endsWith(".yml")
                            && !entry.isDirectory()) {

                        YmlResourceManager.update(this, name);
                    }
                }
            }
        } catch (IOException e) {
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.SYSTEM, "Failed to load resources from folder: " + folder, e);
        }
    }
}