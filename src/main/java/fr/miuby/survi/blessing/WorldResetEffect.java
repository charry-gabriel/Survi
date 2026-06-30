package fr.miuby.survi.blessing;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;

public class WorldResetEffect extends BlessingEffect {

    private final int newFrequency;

    public WorldResetEffect(int newFrequency) {
        this.newFrequency = newFrequency;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        if (newFrequency >= 0) {
            GameManager.getInstance().getWorldResetManager().setResetFrequency(newFrequency);
        }
    }

    @Override
    public void resetEffect(AlphaPlayer player) {
        GameManager.getInstance().getWorldResetManager().setResetFrequency(0);
    }
}