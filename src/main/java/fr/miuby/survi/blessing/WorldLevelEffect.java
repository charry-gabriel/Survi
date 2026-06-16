package fr.miuby.survi.blessing;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;

public class WorldLevelEffect extends BlessingEffect {

    @Override
    public void applyEffect(AlphaPlayer player) {
        GameManager.getInstance().getWorldLevelManager().increment();

        if (player.getPlayer() != null) {
            var ls = GameManager.getInstance().getLangService();
            player.getPlayer().sendMessage(
                    ls.text(player.getPlayer(), "world.level_up",
                            GameManager.getInstance().getWorldLevelManager().getLevel()));
        }
    }

    @Override
    public void resetEffect(AlphaPlayer player) {
        GameManager.getInstance().getWorldLevelManager().decrement();
    }
}