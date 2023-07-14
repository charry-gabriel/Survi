package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.locked_item.LockedToolType;

public class UnlockToolEffect extends BlessingEffect {

    private final LockedToolType itemType;

    public UnlockToolEffect(LockedToolType itemType) {
        this.itemType = itemType;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        GameManager.getInstance().getLockedItemsFactory().unlockToolItem(itemType);
    }
}
