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

    public AlphaPlayer getAlphaPlayer(UUID uuid) {
        for(AlphaPlayer alphaP : GameManager.getInstance().getAlphaPlayers().values()) {
            if(alphaP.getUUID() == uuid) {
                return alphaP;
            }
        }
        GameManager.getInstance().getLogger().warning("player null :/");
        return null;
    }

    public void switchWorld(String world, String pseudo){
        for(AlphaPlayer alphaPlayer : players.values()) {
            if(alphaPlayer.getPlayer() != null) {
                switch (world) {
                    case "village":
                        alphaPlayer.getScoreboard().getTeam("Village").addEntry(pseudo);
                        break;
                    case "wilderness":
                        alphaPlayer.getScoreboard().getTeam("Wilderness").addEntry(pseudo);
                        break;
                    case "wilderness_nether":
                        alphaPlayer.getScoreboard().getTeam("Nether").addEntry(pseudo);
                        break;
                    case "wilderness_the_end":
                        alphaPlayer.getScoreboard().getTeam("End").addEntry(pseudo);
                        break;
                }
            }
        }
    }

    public LockedItemsManager getLockedItemsManager() {
        return lockedItemsManager;
    }
}
