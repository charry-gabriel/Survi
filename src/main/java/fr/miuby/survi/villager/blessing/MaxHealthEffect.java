package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.player.AlphaPlayer;

public class MaxHealthEffect extends BlessingEffect {
    private final int maxHealth;

    public MaxHealthEffect(int health) {
        maxHealth = health;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        player.getAlphaLife().setBlessing(maxHealth);
    }
}
