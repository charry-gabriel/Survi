package fr.miuby.survi18;

import fr.miuby.survi18.database.DatabaseManager;
import fr.miuby.survi18.locked_item.LockedItemsManager;
import fr.miuby.survi18.village.Village;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class GameManager {
    private static GameManager instance = null;
    private Survi18 plugin;

    private Village village;
    private final Map<UUID, AlphaPlayer> players = new HashMap<>();

    private LockedItemsManager lockedItemsManager;

    private final Logger logger = Logger.getLogger("Survi18");

    private DatabaseManager databaseManager;

    private Timer timer;

    private boolean hasNetherAccess = false;
    private boolean hasEndAccess = false;

    private int dispel = 0;

    public static GameManager getInstance(){
        if(instance == null){
            instance = new GameManager();
        }
        return instance;
    }

    public void Init(Survi18 plugin){
        this.plugin = plugin;

        databaseManager = new DatabaseManager();
        databaseManager.createAlphaPlayers();

        village = new Village(this.GetWorld("village"));
        databaseManager.createVillagers(village);

        lockedItemsManager = new LockedItemsManager();

        timer = new Timer();
        timer.update();
    }

    public World GetWorld(String name) {
        return plugin.getServer().getWorld(name);
    }

    public Map<UUID, AlphaPlayer> getAlphaPlayers(){
        return players;
    }

    public Village getVillage() {
        return village;
    }

    public Survi18 getPlugin() {
        return plugin;
    }

    public Logger getLogger() {
        return logger;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public Timer getTimer() {
        return timer;
    }

    public AlphaPlayer getAlphaPlayer(String pseudo) {
        for(AlphaPlayer alphaP : GameManager.getInstance().getAlphaPlayers().values()) {
            if(alphaP.getPseudo().equalsIgnoreCase(pseudo)) {
                return alphaP;
            }
        }
        return null;
    }

    public void switchWorld(String world, String pseudo){
        getLogger().info(pseudo + " has switched to " + world);
        for(AlphaPlayer alphaPlayer : players.values()) {
            if(alphaPlayer.getPlayer() != null) {
                switch (world) {
                    case "Village":
                        alphaPlayer.getScoreboard().getTeam("Village").addEntry(pseudo);
                        break;
                    case "Wilderness":
                        alphaPlayer.getScoreboard().getTeam("Wilderness").addEntry(pseudo);
                        break;
                    case "Wilderness_nether":
                        alphaPlayer.getScoreboard().getTeam("Nether").addEntry(pseudo);
                        break;
                    case "Wilderness_the_end":
                        alphaPlayer.getScoreboard().getTeam("End").addEntry(pseudo);
                        break;
                }
            }
        }
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
}
