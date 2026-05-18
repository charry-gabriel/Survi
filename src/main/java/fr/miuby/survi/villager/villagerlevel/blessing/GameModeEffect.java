package fr.miuby.survi.villager.villagerlevel.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import lombok.RequiredArgsConstructor;
import org.bukkit.GameMode;

@RequiredArgsConstructor
public class GameModeEffect extends BlessingEffect {
    private final GameMode gameMode;

    @Override
    public void applyEffect(VillagerLevel villager, AlphaPlayer player) {
        player.getPlayer().setGameMode(this.gameMode);
    }
}