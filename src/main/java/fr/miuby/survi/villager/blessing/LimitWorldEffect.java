package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.world.WorldFactory;
import fr.miuby.utils.Rect;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.world.EWorld;
import fr.miuby.world.MiubyWorld;

public class LimitWorldEffect extends BlessingEffect {
    private final MiubyWorld world;
    private final Rect rect;

    public LimitWorldEffect(EWorld worldType, Rect rect) {
        this.world = WorldFactory.get(worldType);
        this.rect = rect;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        this.world.setLimit(this.rect);
    }
}
