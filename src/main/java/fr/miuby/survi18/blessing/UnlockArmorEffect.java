package fr.miuby.survi18.blessing;

import fr.miuby.survi18.AlphaPlayer;
import fr.miuby.survi18.GameManager;
import fr.miuby.survi18.locked_item.LockedArmorType;

public class UnlockArmorEffect extends BlessingEffect{

    private LockedArmorType itemType;
    public UnlockArmorEffect(LockedArmorType itemType) {
        this.itemType = itemType;
    }

    @Override
    public void applyEffect(AlphaPlayer player) {
        GameManager.getInstance().getLockedItemsManager().unlockArmorItem(itemType);
    }
}
