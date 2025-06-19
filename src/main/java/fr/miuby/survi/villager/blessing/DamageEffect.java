package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DamageEffect extends BlessingEffect {
    private final float damage;

    @Override
    public void applyEffect(AlphaPlayer player) {
        player.setDamageModifier(damage);
    }
}
