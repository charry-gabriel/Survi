package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.AlphaPlayer;

public class DamageEffect extends BlessingEffect {
    private final float damage;

    public DamageEffect(float damage) {
        this.damage = damage;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        player.setDamage(damage);
    }
}
