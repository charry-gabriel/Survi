package fr.miuby.survi.villager.villagerlevel.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DamageEffect extends BlessingEffect {
    private final float damage;

    @Override
    public void applyEffect(VillagerLevel villager, AlphaPlayer player) {
        player.setDamageModifier(damage);
    }
}
