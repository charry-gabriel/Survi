package fr.miuby.survi.blessing;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.log.ELogTag;

import java.util.logging.Level;

public class WorldResetEffect extends BlessingEffect {

    private final int newFrequency;

    public WorldResetEffect(int newFrequency) {
        this.newFrequency = newFrequency;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        if (newFrequency >= 0) {
            GameManager.getInstance().getWorldResetManager().setResetFrequency(newFrequency);
            MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD, "Fréquence de reset des mondes mise à jour à " + newFrequency + " jour(s).");
        }
    }

    @Override
    public void resetEffect(AlphaPlayer player) {
        GameManager.getInstance().getWorldResetManager().setResetFrequency(0);
        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD, "Fréquence de reset des mondes remise à 0 (reset villager).");
    }
}