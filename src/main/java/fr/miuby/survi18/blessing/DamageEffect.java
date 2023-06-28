package fr.miuby.survi18.blessing;

import fr.miuby.survi18.AlphaPlayer;

public class DamageEffect extends BlessingEffect {
    private final float damage;

    public DamageEffect(float damage) {
        this.damage = damage;
    }

    @Override
    public void ApplyEffect(AlphaPlayer player) {
        player.setDamage(damage);
    }
}
