package fr.miuby.survi18.blessing;

import fr.miuby.survi18.AlphaPlayer;
import fr.miuby.survi18.GameManager;

public class NetherEffect extends BlessingEffect {
    @Override
    public void applyEffect(AlphaPlayer player) {
        GameManager.getInstance().setHasNetherAccess(true);
    }
}
