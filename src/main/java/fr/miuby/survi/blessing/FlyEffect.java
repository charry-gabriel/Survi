package fr.miuby.survi.blessing;

import fr.miuby.survi.player.AlphaPlayer;

public class FlyEffect extends BlessingEffect {
    @Override
    public void applyEffect(AlphaPlayer player) {
        player.getPlayer().setFlying(true);
    }

    @Override
    public boolean requiresOnlinePlayer() { return true; }
}