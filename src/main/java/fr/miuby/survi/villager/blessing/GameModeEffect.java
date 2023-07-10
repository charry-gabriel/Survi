package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.AlphaPlayer;
import org.bukkit.GameMode;

public class GameModeEffect extends BlessingEffect {
    private GameMode gameMode;

    public GameModeEffect(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        player.getPlayer().setGameMode(this.gameMode);
    }
}