package fr.miuby.survi;

import fr.miuby.survi.database.Database;
import fr.miuby.survi.database.SQLite;
import fr.miuby.survi.locked_item.LockedItemsManager;
import fr.miuby.survi.villager.VillagerFactory;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.Monde;
import fr.miuby.survi.world.WorldFactory;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

public class GameManager {
    private static GameManager instance = null;
    private Survi plugin;

    private VillagerFactory villagerFactory;
    private final Map<UUID, AlphaPlayer> players = new HashMap<>();

    private LockedItemsManager lockedItemsManager;

    private final Logger logger = Logger.getLogger("Survi");

    private Timer timer;

    private boolean hasNetherAccess = false;
    private boolean hasEndAccess = false;

    private int dispel = 0;
    private WorldFactory worldFactory;
    private Database database;

    public static GameManager getInstance(){
        if(instance == null){
            instance = new GameManager();
        }
        return instance;
    }

    public void init(Survi plugin){
        this.plugin = plugin;

        this.worldFactory = new WorldFactory(this.plugin.getServer());

        this.database = new SQLite();
        this.database.load();
        this.database.createAlphaPlayers();

        villagerFactory = new VillagerFactory();

        lockedItemsManager = new LockedItemsManager();

        timer = new Timer();
        timer.update();
    }

    public World getWorld(EWorld world) {
        Monde monde = getMonde(world);
        if(monde == null)
            throw new NullPointerException(world.toString() + " Monde doesn't exist !");
        return monde.getWorld();
    }

    public void switchWorld(String world, String pseudo){
        getLogger().info(pseudo + " has switched to " + world);
        for(AlphaPlayer alphaPlayer : players.values()) {
            if(alphaPlayer.getPlayer() != null) {
                switch (world) {
                    case "Village":
                        Objects.requireNonNull(alphaPlayer.getScoreboard().getTeam("Village")).addEntry(pseudo);
                        break;
                    case "Wilderness":
                        Objects.requireNonNull(alphaPlayer.getScoreboard().getTeam("Wilderness")).addEntry(pseudo);
                        break;
                    case "Wilderness_nether":
                        Objects.requireNonNull(alphaPlayer.getScoreboard().getTeam("Nether")).addEntry(pseudo);
                        break;
                    case "Wilderness_the_end":
                        Objects.requireNonNull(alphaPlayer.getScoreboard().getTeam("End")).addEntry(pseudo);
                        break;
                }
            }
        }
    }

    public Monde getMonde(EWorld world) {
        return worldFactory.getMonde(world);
    }

    public Map<UUID, AlphaPlayer> getAlphaPlayers(){
        return players;
    }

    public AlphaPlayer getAlphaPlayer(UUID uuid){
        AlphaPlayer alphaPlayer = players.get(uuid);
        if (alphaPlayer == null)
            throw new NullPointerException(uuid.toString() + " alphaPlayer doesn't exist !");
        return alphaPlayer;
    }

    public VillagerFactory getVillagerFactory() {
        return villagerFactory;
    }

    public Survi getPlugin() {
        return plugin;
    }

    public Logger getLogger() {
        return logger;
    }

    public LockedItemsManager getLockedItemsManager() {
        return lockedItemsManager;
    }

    public boolean hasNetherAccess() {
        return hasNetherAccess;
    }

    public void setHasNetherAccess(boolean hasNetherAccess) {
        this.hasNetherAccess = hasNetherAccess;
    }

    public boolean hasEndAccess() {
        return hasEndAccess;
    }

    public void setHasEndAccess(boolean hasEndAccess) {
        this.hasEndAccess = hasEndAccess;
    }

    public int getDispel() {
        return dispel;
    }

    public void setDispel(int dispel) {
        this.dispel = dispel;
    }

    public Database getDatabase() {
        return database;
    }
}
