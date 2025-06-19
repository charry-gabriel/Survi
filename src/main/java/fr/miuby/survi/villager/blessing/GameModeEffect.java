package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import lombok.RequiredArgsConstructor;
import org.bukkit.GameMode;

@RequiredArgsConstructor
public class GameModeEffect extends BlessingEffect {
    private final GameMode gameMode;

    @Override
    public void applyEffect(AlphaPlayer player) {
        player.getPlayer().setGameMode(this.gameMode);
    }
}