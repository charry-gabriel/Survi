package fr.miuby.survi18.blessing;

import fr.miuby.survi18.AlphaPlayer;

public class FlyEffect extends BlessingEffect {
    @Override
    public void applyEffect(AlphaPlayer player) {
        player.getPlayer().setFlying(true);
    }
}
