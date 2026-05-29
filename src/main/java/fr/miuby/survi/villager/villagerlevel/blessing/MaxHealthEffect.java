package fr.miuby.survi.villager.villagerlevel.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MaxHealthEffect extends BlessingEffect {
    private final int maxHealth;

    @Override
    public void applyEffect(VillagerLevel villager, AlphaPlayer player) {
        player.getAlphaLife().setBlessing(maxHealth);
    }

    @Override
    public void resetEffect(VillagerLevel villager, AlphaPlayer player) {
        player.getAlphaLife().setBlessing(0);
    }
}