package fr.miuby.survi.villager.villagerlevel.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.locked_item.ELockedArmorType;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UnlockArmorEffect extends BlessingEffect{
    private final ELockedArmorType itemType;

    @Override
    public void applyEffect(VillagerLevel villager, AlphaPlayer player) {
        GameManager.getInstance().getLockedItemsFactory().unlockArmorItem(player, itemType);
    }
}
