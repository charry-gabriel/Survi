package fr.miuby.survi;

import fr.miuby.survi.database.Database;
import fr.miuby.survi.database.SQLite;
import fr.miuby.survi.locked_item.LockedItemsManager;
import fr.miuby.survi.village.Village;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

public class GameManager {
    private static GameManager instance = null;
    private Survi plugin;

    private Village village;
    private final Map<UUID, AlphaPlayer> players = new HashMap<>();

    private LockedItemsManager lockedItemsManager;

    private final Logger logger = Logger.getLogger("Survi");

    private Timer timer;

    private boolean hasNetherAccess = false;
    private boolean hasEndAccess = false;

    private int dispel = 0;
    private Database database;

    public static GameManager getInstance(){
        if(instance == null){
            instance = new GameManager();
        }
        return instance;
    }

    public void Init(Survi plugin){
        this.plugin = plugin;

        this.database = new SQLite();
        this.database.load();
        this.database.createAlphaPlayers();

        village = new Village(this.GetWorld("village"));

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

    public Survi getPlugin() {
        return plugin;
    }

    public Logger getLogger() {
        return logger;
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
