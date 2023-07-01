package fr.miuby.survi18.blessing;

import fr.miuby.survi18.AlphaPlayer;
import fr.miuby.survi18.GameManager;

public class DispelEffect extends BlessingEffect{
    private int dispel;

    public DispelEffect(int dispelLevel) {
        dispel = dispelLevel;
    }
    @Override
    public void applyEffect(AlphaPlayer player) {
        GameManager.getInstance().setDispel(dispel);
        player.updateLife();
    }
}
