package fr.miuby.survi.villager.blessing;

import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.world.EWorld;
import fr.miuby.lib.world.MiubyWorld;

public class LockWorldEffect extends BlessingEffect {
    private final MiubyWorld world;

    public LockWorldEffect(EWorld worldType) {
        this.world = WorldRegistry.get(worldType);
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        this.world.setLocked(false);
    }
}
