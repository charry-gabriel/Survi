package fr.miuby.survi18.blessing;

import fr.miuby.survi18.AlphaPlayer;

public class MaxHealthEffect extends BlessingEffect {
    private final int maxHealth;

    public MaxHealthEffect(int health) {
        maxHealth = health;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        player.setVieBonus(maxHealth);
        player.updateLife();
    }
}
