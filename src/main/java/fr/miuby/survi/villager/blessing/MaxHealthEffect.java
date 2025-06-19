package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MaxHealthEffect extends BlessingEffect {
    private final int maxHealth;

    @Override
    public void applyEffect(AlphaPlayer player) {
        player.getAlphaLife().setBlessing(maxHealth);
    }
}
