package fr.miuby.survi;

import fr.miuby.lib.MiubyLib;
import fr.miuby.survi.crops.PlantedCropsManager;
import fr.miuby.survi.role.RoleManagementService;
import fr.miuby.survi.system.database.Database;
import fr.miuby.survi.system.database.SQLite;
import fr.miuby.survi.item.CustomRecipeFactory;
import fr.miuby.survi.item.CustomRecipe;
import fr.miuby.survi.item.growth_item.GrowthItems;
import fr.miuby.survi.item.locked_item.LockedItemsFactory;
import fr.miuby.survi.player.AlphaPlayerFactory;
import fr.miuby.survi.role.RoleRegistry;
import fr.miuby.survi.system.time.TimeManager;
import fr.miuby.survi.villager.VillagerFactory;
import fr.miuby.survi.display.TabDisplayManager;
import fr.miuby.survi.world.WorldInitializer;
import fr.miuby.survi.system.log.LogManager;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.logging.Level;

public class GameManager {
    private static GameManager instance = null;

    @Getter
    private Survi plugin;

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
    @Getter
    private TimeManager timeManager;
    @Getter
    private RoleManagementService roleManagementService;

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

    private GameManager(){}

    public void init(Survi plugin) {
        this.plugin = plugin;
        this.scheduler = this.plugin.getServer().getScheduler();

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.SYSTEM, "Initialisation du plugin...");

        this.initDatabase();
        MiubyLib.init(plugin);
        LogManager.getInstance().initialize();

        this.initWorlds();
    }

    private void initDatabase() {
        if (this.initState != InitState.NOT_INITIALIZED)
            throw new IllegalStateException("Wrong init order !");

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.SYSTEM, "Chargement de la base de données...");
        this.database = new SQLite();
        this.database.load();
        this.initState = InitState.DATABASE_LOADED;
        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.SYSTEM, "Base de données chargée");
    }

    private void initWorlds() {
        if (this.initState != InitState.DATABASE_LOADED)
            throw new IllegalStateException("Wrong init order !");

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.WORLD, "Initialisation (si besoin) des mondes...");
        WorldInitializer.initializeIfNeeded();
        this.initState = InitState.WORLDS_LOADED;
        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.WORLD, "Mondes prêts");
    }

    public void initAfterWorldsLoad() {
        if (this.initState != InitState.WORLDS_LOADED)
            throw new IllegalStateException("Wrong init order !");

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.WORLD, "Chargement complet des mondes...");
        WorldInitializer.initializeWorlds();

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.PLAYER, "Initialisation des joueurs...");
        this.initPlayers();

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.VILLAGER, "Initialisation des villageois...");
        this.villagerFactory = new VillagerFactory();

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.ITEM, "Initialisation des items/recettes...");
        this.initItems();

        this.plantedCropsManager = new PlantedCropsManager(database);
        this.plantedCropsManager.load();

        this.timeManager = new TimeManager();
        timeManager.start();

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.SYSTEM, "Plugin entièrement initialisé !");
    }

    private void initPlayers() {
        this.roleRegistry = new RoleRegistry();
        this.alphaPlayerFactory = new AlphaPlayerFactory();
        this.database.players().createAlphaPlayers();

        this.tabDisplayManager = new TabDisplayManager();
        this.roleManagementService = new RoleManagementService(alphaPlayerFactory.getPersistenceService(), alphaPlayerFactory);
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
