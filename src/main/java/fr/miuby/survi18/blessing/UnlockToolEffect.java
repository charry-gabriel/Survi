package fr.miuby.survi18.blessing;

import fr.miuby.survi18.AlphaPlayer;
import fr.miuby.survi18.GameManager;
import fr.miuby.survi18.locked_item.LockedToolType;

public class UnlockToolEffect extends BlessingEffect {

    private final LockedToolType itemType;

    public UnlockToolEffect(LockedToolType itemType) {
        this.itemType = itemType;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        GameManager.getInstance().getLockedItemsManager().unlockToolItem(itemType);
    }
}
