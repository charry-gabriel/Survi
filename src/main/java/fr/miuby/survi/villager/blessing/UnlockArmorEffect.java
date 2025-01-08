package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.locked_item.LockedArmorType;

public class UnlockArmorEffect extends BlessingEffect{

    private final LockedArmorType itemType;
    public UnlockArmorEffect(LockedArmorType itemType) {
        this.itemType = itemType;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        GameManager.getInstance().getLockedItemsFactory().unlockArmorItem(player, itemType);
    }
}
