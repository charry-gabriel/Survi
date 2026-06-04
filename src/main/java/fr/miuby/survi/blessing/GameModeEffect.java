package fr.miuby.survi.blessing;

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

    @Override
    public boolean requiresOnlinePlayer() { return true; }
}