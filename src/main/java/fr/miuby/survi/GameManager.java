package fr.miuby.survi;

import fr.miuby.lib.MiubyLib;
import fr.miuby.survi.crops.PlantedCropsManager;
import fr.miuby.survi.database.Database;
import fr.miuby.survi.database.SQLite;
import fr.miuby.survi.item.CustomRecipeFactory;
import fr.miuby.survi.item.CustomRecipe;
import fr.miuby.survi.item.growth_item.GrowthItems;
import fr.miuby.survi.item.locked_item.LockedItemsFactory;
import fr.miuby.survi.player.AlphaPlayerFactory;
import fr.miuby.survi.role.RoleRegistry;
import fr.miuby.survi.villager.VillagerFactory;
import fr.miuby.survi.display.TabDisplayManager;
import fr.miuby.survi.world.WorldInitializer;
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
    private RoleRegistry roleRegistry;
    @Getter
    private AlphaPlayerFactory alphaPlayerFactory;
    @Getter
    private PlantedCropsManager plantedCropsManager;
    @Getter
    private TabDisplayManager tabDisplayManager;

    @Setter
    @Getter
    private int dispel = 0;
    @Setter
    @Getter
    private boolean isNight;

    private enum InitState {
        NOT_INITIALIZED,
        DATABASE_LOADED,
        WORLDS_LOADED
    }
    private InitState initState = InitState.NOT_INITIALIZED;

    public static GameManager getInstance(){
        if(instance == null){
            instance = new GameManager();
        }
        return instance;
    }

    public void init(Survi plugin) {
        this.plugin = plugin;
        this.scheduler = this.plugin.getServer().getScheduler();

        this.initDatabase();
        MiubyLib.init(plugin);

        this.initWorlds();
    }

    private void initDatabase() {
        if (this.initState != InitState.NOT_INITIALIZED)
            throw new IllegalStateException("Wrong init order !");

        this.database = new SQLite();
        this.database.load();
        this.initState = InitState.DATABASE_LOADED;
    }

    private void initWorlds() {
        if (this.initState != InitState.DATABASE_LOADED)
            throw new IllegalStateException("Wrong init order !");

        WorldInitializer.initializeIfNeeded();
        this.initState = InitState.WORLDS_LOADED;
    }

    public void initAfterWorldsLoad() {
        if (this.initState != InitState.WORLDS_LOADED)
            throw new IllegalStateException("Wrong init order !");

        WorldInitializer.initializeWorlds();

        this.initPlayers();

        this.villagerFactory = new VillagerFactory();

        this.initItems();

        this.plantedCropsManager = new PlantedCropsManager(database);
        this.plantedCropsManager.load();

        Timer timer = new Timer();
        timer.update();
        
        plugin.getLogger().info("Plugin entièrement initialisé !");
    }

    private void initPlayers() {
        this.roleRegistry = new RoleRegistry();
        this.alphaPlayerFactory = new AlphaPlayerFactory();
        this.database.createAlphaPlayers();

        this.tabDisplayManager = new TabDisplayManager();
    }

    private void initItems() {
        this.lockedItemsFactory = new LockedItemsFactory();

        this.customRecipeFactory = new CustomRecipeFactory();
        CustomRecipe.registerRecipes();

        GrowthItems.init();
    }

    public void callEvent(Event event) {
        this.plugin.getServer().getPluginManager().callEvent(event);
    }
}
