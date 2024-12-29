package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.utils.Rect;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.Monde;

public class LimitWorldEffect extends BlessingEffect {
    private final Monde world;
    private final Rect rect;

    public LimitWorldEffect(EWorld worldType, Rect rect) {
        this.world = Monde.get(worldType);
        this.rect = rect;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        this.world.setLimit(this.rect);
    }
}
