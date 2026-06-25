package fr.miuby.survi.blessing;

import fr.miuby.survi.GameManager;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.log.ELogTag;

import java.util.logging.Level;

public class WorldLevelEffect extends BlessingEffect {

    @Override
    public void applyEffect(AlphaPlayer player) {
        GameManager.getInstance().getWorldLevelManager().increment();
        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                "[WorldLevelEffect] increment déclenché par " + player.getPseudo()
                        + " → niveau " + GameManager.getInstance().getWorldLevelManager().getLevel());

        if (player.getPlayer() != null) {
            var ls = GameManager.getInstance().getLangService();
            player.getPlayer().sendMessage(
                    ls.text(player.getPlayer(), "world.level_up.event",
                            GameManager.getInstance().getWorldLevelManager().getLevel()));
        }
    }

    @Override
    public void resetEffect(AlphaPlayer player) {
        GameManager.getInstance().getWorldLevelManager().decrement();
    }

    /** One-shot : l'incrément est persisté en DB, on ne le rejoue pas à la reconnexion. */
    @Override
    public boolean isOneShot() { return true; }
}