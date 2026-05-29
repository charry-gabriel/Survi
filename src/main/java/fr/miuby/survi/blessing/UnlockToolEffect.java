package fr.miuby.survi.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.locked_item.ELockedToolType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UnlockToolEffect extends BlessingEffect {
    private final ELockedToolType itemType;

    @Override
    public void applyEffect(AlphaPlayer player) {
        GameManager.getInstance().getLockedItemsFactory().unlockToolItem(player, itemType);
    }

    @Override
    public void resetEffect(AlphaPlayer player) {
        GameManager.getInstance().getLockedItemsFactory().lockToolItem(itemType);
    }
}