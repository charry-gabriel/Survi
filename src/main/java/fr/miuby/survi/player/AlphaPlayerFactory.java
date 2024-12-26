package fr.miuby.survi.player;

import fr.miuby.survi.GameManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AlphaPlayerFactory {
    private final Map<UUID, AlphaPlayer> players = new HashMap<>();

    public Map<UUID, AlphaPlayer> getAlphaPlayers(){
        return players;
    }

    public AlphaPlayer getAlphaPlayer(UUID uuid){
        AlphaPlayer alphaPlayer = players.get(uuid);
        if (alphaPlayer == null)
            throw new NullPointerException(uuid.toString() + " alphaPlayer doesn't exist !");
        return alphaPlayer;
    }

    public void setPlayersToTeam(AlphaScoreboard scoreboard) {
        for(AlphaPlayer alphaPlayer : players.values()) {
            if(alphaPlayer.getPlayer() != null) {
                scoreboard.getTeam(alphaPlayer.getWorld(), alphaPlayer.getRole().getType()).addPlayer(alphaPlayer);
                GameManager.getInstance().getLogger().info(alphaPlayer.getPlayer().getName() + " est ajouté a mon scoreboard !");
            }
        }
    }

    public void sendToPlayers(AlphaPlayer player) {
        for(AlphaPlayer alphaPlayer : players.values()) {
            if(alphaPlayer.getPlayer() != null) {
                alphaPlayer.getScoreboard().getTeam(player.getWorld(), player.getRole().getType()).addPlayer(player);
            }
        }
    }

    public void playerJoin(UUID uuid) {
        if(!players.containsKey(uuid)) {
            AlphaPlayer alphaPlayer = new AlphaPlayer(uuid);
            players.put(uuid, alphaPlayer);
            GameManager.getInstance().getDatabase().getAlphaPlayer(alphaPlayer, uuid);
        }else{
            AlphaPlayer.get(uuid).joinServer();
        }
    }
}
