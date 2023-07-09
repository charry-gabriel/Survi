package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.AlphaPlayer;
import fr.miuby.survi.GameManager;

public class DispelEffect extends BlessingEffect{
    private final int dispel;

    public DispelEffect(int dispelLevel) {
        dispel = dispelLevel;
    }
    @Override
    public void applyEffect(AlphaPlayer player) {
        GameManager.getInstance().setDispel(dispel);
        player.updateLife();
    }
}
