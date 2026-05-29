package fr.miuby.survi.blessing;

import fr.miuby.survi.player.AlphaPlayer;

public abstract class BlessingEffect {
    public abstract void applyEffect(AlphaPlayer player);

    public void resetEffect(AlphaPlayer player) {
        // no-op par défaut
    }
}