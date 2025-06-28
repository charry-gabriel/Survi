package fr.miuby.survi.villager.blessing;

import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.world.EWorld;
import fr.miuby.lib.world.MLWorld;

public class LockWorldEffect extends BlessingEffect {
    private final MLWorld world;

    public LockWorldEffect(EWorld worldType) {
        this.world = WorldRegistry.get(worldType);
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        this.world.setLocked(false);
    }
}
