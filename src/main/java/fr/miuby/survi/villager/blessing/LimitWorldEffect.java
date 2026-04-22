package fr.miuby.survi.villager.blessing;

import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.lib.utils.Rect;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.VillagerLevel;
import fr.miuby.survi.world.EWorld;

public class LimitWorldEffect extends BlessingEffect {
    private final EWorld worldType;
    private final Rect rect;

    public LimitWorldEffect(EWorld worldType, Rect rect) {
        this.worldType = worldType;
        this.rect = rect;
    }

    @Override
    public void applyEffect(VillagerLevel villager, AlphaPlayer player) {
        WorldRegistry.get(worldType).setLimit(this.rect);
    }
}