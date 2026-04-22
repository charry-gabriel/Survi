package fr.miuby.survi.villager.blessing;

import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.VillagerLevel;
import fr.miuby.survi.world.EWorld;

public class LockWorldEffect extends BlessingEffect {
    private final EWorld worldType;

    public LockWorldEffect(EWorld worldType) {
        this.worldType = worldType;
    }

    @Override
    public void applyEffect(VillagerLevel villager, AlphaPlayer player) {
        WorldRegistry.get(worldType).setLocked(false);
    }
}