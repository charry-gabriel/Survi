package fr.miuby.survi;

import fr.miuby.lib.MiubyLib;
import fr.miuby.survi.grave.GraveManager;
import fr.miuby.survi.mob.MobLevelManager;
import fr.miuby.survi.world.crops.PlantedCropsManager;
import fr.miuby.survi.display.TabDisplayManager;
import fr.miuby.survi.quest.GlobalQuestManager;
import fr.miuby.survi.quest.QuestManager;
import fr.miuby.survi.role.RoleManagementService;
import fr.miuby.survi.system.database.Database;
import fr.miuby.survi.system.database.SQLite;
import fr.miuby.survi.item.CustomRecipeFactory;
import fr.miuby.survi.item.CustomRecipe;
import fr.miuby.survi.item.growth_item.GrowthItems;
import fr.miuby.survi.item.locked_item.LockedItemsFactory;
import fr.miuby.survi.player.AlphaPlayerFactory;
import fr.miuby.survi.role.RoleLoader;
import fr.miuby.survi.system.time.TimeManager;
import fr.miuby.survi.villager.VillagerFactory;
import fr.miuby.survi.world.WorldLevelManager;
import fr.miuby.survi.world.WorldPortalManager;
import fr.miuby.survi.world.WorldResetManager;
import fr.miuby.survi.world.VillageZoneManager;
import fr.miuby.survi.system.SurviConfig;
import fr.miuby.survi.world.WorldInitializer;
import fr.miuby.survi.system.log.LogManager;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.logging.Level;

public class GameManager {
    private static GameManager instance = null;

    @Getter private Survi plugin;
    @Getter private BukkitScheduler scheduler;
    @Getter private VillagerFactory villagerFactory;
    @Getter private LockedItemsFactory lockedItemsFactory;
    @Getter private CustomRecipeFactory customRecipeFactory;
    @Getter private Database database;
    @Getter private RoleLoader roleLoader;
    @Getter private AlphaPlayerFactory alphaPlayerFactory;
    @Getter private PlantedCropsManager plantedCropsManager;
    @Getter private TabDisplayManager tabDisplayManager;
    @Getter private TimeManager timeManager;
    @Getter private RoleManagementService roleManagementService;
    @Getter private QuestManager questManager;
    @Getter private GlobalQuestManager globalQuestManager;
    @Getter private GraveManager graveManager;
    @Getter private WorldLevelManager worldLevelManager;
    @Getter private MobLevelManager mobLevelManager;
    @Getter private WorldPortalManager worldPortalManager;
    @Getter private WorldResetManager worldResetManager;
    @Getter private VillageZoneManager villageZoneManager;

    @Setter @Getter private int dispel = 0;
    @Setter @Getter private boolean isNight;

    /**
     * Suit l'ordre d'initialisation obligatoire du plugin.
     *
     * Séquence attendue :
     *   1. {@link #init(Survi)}              → NOT_STARTED → DATABASE_READY
     *   2. {@link #initAfterWorldsLoad()}    → DATABASE_READY → FULLY_LOADED
     *      (déclenché par WorldInitializer une fois les mondes prêts)
     */
    private enum EInitState {
        NOT_STARTED,
        DATABASE_READY,
        FULLY_LOADED
    }
    private EInitState initState = EInitState.NOT_STARTED;

    public static GameManager getInstance() {
        if (instance == null) instance = new GameManager();
        return instance;
    }

    private GameManager() {}

    public void init(Survi plugin) {
        SurviConfig.getInstance().init(plugin);
        this.plugin = plugin;
        this.scheduler = this.plugin.getServer().getScheduler();

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.SYSTEM, "Initialisation du plugin...");

        this.initDatabase();
        MiubyLib.init(plugin);
        LogManager.getInstance().initialize();

        this.initWorlds();
    }

    private void initDatabase() {
        if (this.initState != EInitState.NOT_STARTED)
            throw new IllegalStateException("Wrong init order !");

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.SYSTEM, "Chargement de la base de données...");
        this.database = new SQLite();
        this.database.load();
        this.initState = EInitState.DATABASE_READY;
        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.SYSTEM, "Base de données chargée");
    }

    private void initWorlds() {
        if (this.initState != EInitState.DATABASE_READY)
            throw new IllegalStateException("Wrong init order !");

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.WORLD, "Initialisation (si besoin) des mondes...");
        this.initState = EInitState.FULLY_LOADED;

        WorldInitializer.initializeIfNeeded();
    }

    public void initAfterWorldsLoad() {
        if (this.initState != EInitState.FULLY_LOADED)
            throw new IllegalStateException("Wrong init order !");

        this.worldLevelManager = new WorldLevelManager();
        this.worldLevelManager.load();

        this.worldPortalManager = new WorldPortalManager();
        this.worldPortalManager.init();

        this.villageZoneManager = new VillageZoneManager();
        this.villageZoneManager.init();

        this.worldResetManager = new WorldResetManager();

        this.mobLevelManager = new MobLevelManager();
        this.mobLevelManager.init();

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.PLAYER, "Initialisation des joueurs...");
        this.initPlayers();

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.VILLAGER, "Initialisation des villageois...");
        this.villagerFactory = new VillagerFactory();

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.ITEM, "Initialisation des items/recettes...");
        this.initItems();

        this.questManager = new QuestManager();
        this.globalQuestManager = new GlobalQuestManager();

        this.graveManager = new GraveManager();

        this.plantedCropsManager = new PlantedCropsManager(database);
        this.plantedCropsManager.load();

        this.timeManager = new TimeManager();
        timeManager.start();

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.SYSTEM, "Plugin entièrement initialisé !");
    }

    private void initPlayers() {
        this.roleLoader = new RoleLoader();
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
