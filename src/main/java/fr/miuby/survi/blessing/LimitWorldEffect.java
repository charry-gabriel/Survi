package fr.miuby.survi.blessing;

import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.lib.utils.Rect;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.world.EWorld;

public class LimitWorldEffect extends BlessingEffect {
    private final EWorld worldType;
    private final Rect rect;

    public LimitWorldEffect(EWorld worldType, Rect rect) {
        this.worldType = worldType;
        this.rect = rect;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        WorldRegistry.get(worldType).setLimit(this.rect);
    }

    @Override
    public void resetEffect(AlphaPlayer player) {
        WorldRegistry.get(worldType).setLimit(null);
    }
}