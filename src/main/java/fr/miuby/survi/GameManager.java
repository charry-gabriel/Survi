package fr.miuby.survi;

import fr.miuby.survi.crops.PlantedCropsManager;
import fr.miuby.survi.database.Database;
import fr.miuby.survi.database.SQLite;
import fr.miuby.survi.item.CustomRecipeFactory;
import fr.miuby.survi.item.CustomRecipe;
import fr.miuby.survi.item.growth_item.GrowthItems;
import fr.miuby.survi.item.locked_item.LockedItemsFactory;
import fr.miuby.survi.player.AlphaPlayerFactory;
import fr.miuby.survi.role.RoleFactory;
import fr.miuby.survi.villager.VillagerFactory;
import fr.miuby.survi.world.WorldFactory;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.logging.Logger;

public class GameManager {
    private static GameManager instance = null;

    @Getter
    private Survi plugin;
    @Getter
    private final Logger logger = Logger.getLogger("Survi");
    @Getter
    private BukkitScheduler scheduler;
    @Getter
    private VillagerFactory villagerFactory;
    @Getter
    private LockedItemsFactory lockedItemsFactory;
    @Getter
    private CustomRecipeFactory customRecipeFactory;
    @Getter
    private Database database;
    @Getter
    private RoleFactory roleFactory;
    @Getter
    private AlphaPlayerFactory alphaPlayerFactory;
    @Getter
    private PlantedCropsManager plantedCropsManager;

    @Setter
    @Getter
    private int dispel = 0;
    @Setter
    @Getter
    private boolean isNight;

    public static GameManager getInstance(){
        if(instance == null){
            instance = new GameManager();
        }
        return instance;
    }

    public void init(Survi plugin) {
        this.plugin = plugin;
        this.scheduler = this.plugin.getServer().getScheduler();

        this.database = new SQLite();
        this.database.load();
    }

    public void initAfterWorldsLoad() {
        WorldFactory.initializeWorlds(plugin.getServer());
        plugin.getLogger().info("Mondes initialisés avec succès !");

        this.roleFactory = new RoleFactory();
        this.alphaPlayerFactory = new AlphaPlayerFactory();
        this.database.createAlphaPlayers();

        this.villagerFactory = new VillagerFactory();

        this.lockedItemsFactory = new LockedItemsFactory();
        this.customRecipeFactory = new CustomRecipeFactory();
        CustomRecipe.registerRecipes();
        GrowthItems.init();

        this.plantedCropsManager = new PlantedCropsManager(database);
        this.plantedCropsManager.load();

        Timer timer = new Timer();
        timer.update();
        
        plugin.getLogger().info("Plugin entièrement initialisé !");
    }

    public void callEvent(Event event) {
        this.plugin.getServer().getPluginManager().callEvent(event);
    }
}
