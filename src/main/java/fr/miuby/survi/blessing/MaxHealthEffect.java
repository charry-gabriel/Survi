package fr.miuby.survi.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MaxHealthEffect extends BlessingEffect {
    private final int maxHealth;

    @Override
    public void applyEffect(AlphaPlayer player) {
        player.getAlphaLife().setBlessing(maxHealth);
    }

    @Override
    public void resetEffect(AlphaPlayer player) {
        player.getAlphaLife().setBlessing(0);
    }
}