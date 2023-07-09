package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.AlphaPlayer;
import fr.miuby.survi.GameManager;

public class NetherEffect extends BlessingEffect {
    @Override
    public void applyEffect(AlphaPlayer player) {
        GameManager.getInstance().setHasNetherAccess(true);
    }
}
