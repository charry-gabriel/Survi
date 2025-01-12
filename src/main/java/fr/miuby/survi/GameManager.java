package fr.miuby.survi;

import fr.miuby.survi.database.Database;
import fr.miuby.survi.database.SQLite;
import fr.miuby.survi.item.CustomRecipeFactory;
import fr.miuby.survi.item.CustomRecipe;
import fr.miuby.survi.item.locked_item.LockedItemsFactory;
import fr.miuby.survi.player.AlphaPlayerFactory;
import fr.miuby.survi.role.RoleFactory;
import fr.miuby.survi.villager.VillagerFactory;
import fr.miuby.survi.world.WorldFactory;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.logging.Logger;

public class GameManager {
    private static GameManager instance = null;
    private Survi plugin;
    private final Logger logger = Logger.getLogger("Survi");
    private BukkitScheduler scheduler;

    private int dispel = 0;
    private boolean isNight;

    //region Factory
    private VillagerFactory villagerFactory;
    private LockedItemsFactory lockedItemsFactory;
    private CustomRecipeFactory customRecipeFactory;
    private WorldFactory worldFactory;
    private Database database;
    private RoleFactory roleFactory;
    private AlphaPlayerFactory alphaPlayerFactory;
    //endregion

    public static GameManager getInstance(){
        if(instance == null){
            instance = new GameManager();
        }
        return instance;
    }

    public void init(Survi plugin){
        this.plugin = plugin;

        this.scheduler = this.plugin.getServer().getScheduler();

        this.worldFactory = new WorldFactory(this.plugin.getServer());
        this.roleFactory = new RoleFactory();
        this.alphaPlayerFactory = new AlphaPlayerFactory();

        this.database = new SQLite();
        this.database.load();
        this.database.createAlphaPlayers();

        villagerFactory = new VillagerFactory();

        lockedItemsFactory = new LockedItemsFactory();
        customRecipeFactory = new CustomRecipeFactory();
        CustomRecipe.registerRecipes();

        Timer timer = new Timer();
        timer.update();
    }

    public int getDispel() {
        return dispel;
    }

    public void setDispel(int dispel) {
        this.dispel = dispel;
    }

    public boolean isNight() {
        return isNight;
    }

    public void setNight(boolean night) {
        isNight = night;
    }

    //region Factory
    public VillagerFactory getVillagerFactory() {
        return villagerFactory;
    }

    public LockedItemsFactory getLockedItemsFactory() {
        return lockedItemsFactory;
    }

    public CustomRecipeFactory getCustomItemFactory() {
        return customRecipeFactory;
    }

    public RoleFactory getRoleFactory() {
        return roleFactory;
    }

    public WorldFactory getWorldFactory() {
        return worldFactory;
    }

    public AlphaPlayerFactory getAlphaPlayerFactory() {
        return alphaPlayerFactory;
    }
    //endregion

    public Survi getPlugin() {
        return plugin;
    }

    public Logger getLogger() {
        return logger;
    }

    public Database getDatabase() {
        return database;
    }

    public BukkitScheduler getScheduler() {
        return scheduler;
    }
}
