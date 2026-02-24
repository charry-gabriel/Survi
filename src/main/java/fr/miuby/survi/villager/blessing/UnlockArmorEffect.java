package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.locked_item.LockedArmorType;
import fr.miuby.survi.villager.VillagerLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UnlockArmorEffect extends BlessingEffect{
    private final LockedArmorType itemType;

    @Override
    public void applyEffect(VillagerLevel villager, AlphaPlayer player) {
        GameManager.getInstance().getLockedItemsFactory().unlockArmorItem(player, itemType);
    }
}
