package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.log.LogManager;
import fr.miuby.survi.villager.VillagerLevel;
import fr.miuby.survi.world.WorldResetManager;

import java.util.logging.Level;

public class WorldResetEffect extends BlessingEffect {

    private final int newFrequency;

    public WorldResetEffect(int newFrequency) {
        this.newFrequency = newFrequency;
    }

    @Override
    public void applyEffect(VillagerLevel villager, AlphaPlayer player) {
        if (newFrequency >= 0) {
            WorldResetManager.getInstance().setResetFrequency(newFrequency);
            LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.WORLD, "Fréquence de reset des mondes mise à jour à " + newFrequency + " jour(s).");
        }
    }
}