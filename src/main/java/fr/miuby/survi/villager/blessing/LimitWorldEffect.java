package fr.miuby.survi.villager.blessing;

import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.lib.utils.Rect;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.world.EWorld;
import fr.miuby.lib.world.MiubyWorld;

public class LimitWorldEffect extends BlessingEffect {
    private final MiubyWorld world;
    private final Rect rect;

    public LimitWorldEffect(EWorld worldType, Rect rect) {
        this.world = WorldRegistry.get(worldType);
        this.rect = rect;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        this.world.setLimit(this.rect);
    }
}
