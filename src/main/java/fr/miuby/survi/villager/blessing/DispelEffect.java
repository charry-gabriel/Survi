package fr.miuby.survi.villager.blessing;

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
}
