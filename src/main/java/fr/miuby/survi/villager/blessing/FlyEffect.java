package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.AlphaPlayer;

public class FlyEffect extends BlessingEffect {
    @Override
    public void applyEffect(AlphaPlayer player) {
        player.getPlayer().setFlying(true);
    }
}
