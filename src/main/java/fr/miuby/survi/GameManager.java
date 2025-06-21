package fr.miuby.survi;

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
    private WorldFactory worldFactory;
    @Getter
    private Database database;
    @Getter
    private RoleFactory roleFactory;
    @Getter
    private AlphaPlayerFactory alphaPlayerFactory;

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
        GrowthItems.init();

        Timer timer = new Timer();
        timer.update();
    }

    public void callEvent(Event event) {
        this.plugin.getServer().getPluginManager().callEvent(event);
    }
}
