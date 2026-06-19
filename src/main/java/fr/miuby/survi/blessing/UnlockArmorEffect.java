package fr.miuby.survi.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.locked_item.ELockedArmorType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UnlockArmorEffect extends BlessingEffect{
    private final ELockedArmorType itemType;

    @Override
    public void applyEffect(AlphaPlayer player) {
        GameManager.getInstance().getLockedItemsFactory().unlockArmorItem(player, itemType);
    }

    @Override
    public void resetEffect(AlphaPlayer player) {
        GameManager.getInstance().getLockedItemsFactory().lockArmorItem(player, itemType);
    }
}