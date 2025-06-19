package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.locked_item.LockedToolType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UnlockToolEffect extends BlessingEffect {
    private final LockedToolType itemType;

    @Override
    public void applyEffect(AlphaPlayer player) {
        GameManager.getInstance().getLockedItemsFactory().unlockToolItem(player, itemType);
    }
}
