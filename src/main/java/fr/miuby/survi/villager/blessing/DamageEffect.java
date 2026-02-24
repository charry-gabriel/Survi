package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.VillagerLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DamageEffect extends BlessingEffect {
    private final float damage;

    @Override
    public void applyEffect(VillagerLevel villager, AlphaPlayer player) {
        player.setDamageModifier(damage);
    }
}
