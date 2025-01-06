package fr.miuby.survi;

import fr.miuby.survi.database.Database;
import fr.miuby.survi.database.SQLite;
import fr.miuby.survi.item.CustomItemFactory;
import fr.miuby.survi.item.CustomRecipe;
import fr.miuby.survi.item.locked_item.LockedItemsFactory;
import fr.miuby.survi.player.AlphaPlayerFactory;
import fr.miuby.survi.player.attribute.AttributeFactory;
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
    private CustomItemFactory customItemFactory;
    private WorldFactory worldFactory;
    private Database database;
    private RoleFactory roleFactory;
    private AlphaPlayerFactory alphaPlayerFactory;
    private AttributeFactory attributeFactory;
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
        this.attributeFactory = new AttributeFactory();
        this.alphaPlayerFactory = new AlphaPlayerFactory();

        this.database = new SQLite();
        this.database.load();
        this.database.createAlphaPlayers();

        villagerFactory = new VillagerFactory();

        lockedItemsFactory = new LockedItemsFactory();
        customItemFactory = new CustomItemFactory();
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

    public CustomItemFactory getCustomItemFactory() {
        return customItemFactory;
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

    public AttributeFactory getLifeFactory() {
        return attributeFactory;
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
