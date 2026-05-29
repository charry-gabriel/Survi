package fr.miuby.survi.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.GameManager;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DispelEffect extends BlessingEffect{
    private final int dispel;

    @Override
    public void applyEffect(AlphaPlayer player) {
        GameManager.getInstance().setDispel(dispel);
        player.getAlphaLife().actualizeDeath();
    }

    @Override
    public void resetEffect(AlphaPlayer player) {
        GameManager.getInstance().setDispel(0);
        player.getAlphaLife().actualizeDeath();
    }
}