package fr.miuby.survi.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.SurviConfig;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DamageEffect extends BlessingEffect {
    private final float damage;

    @Override
    public void applyEffect(AlphaPlayer player) {
        player.setDamageModifier(damage);
    }

    @Override
    public void resetEffect(AlphaPlayer player) {
        player.setDamageModifier(SurviConfig.getInstance().getNormalDamageModifier());
    }
}