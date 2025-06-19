package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.Monde;

public class LockWorldEffect extends BlessingEffect {
    private final Monde world;

    public LockWorldEffect(EWorld worldType) {
        this.world = Monde.get(worldType);
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        this.world.setLocked(false);
    }
}
