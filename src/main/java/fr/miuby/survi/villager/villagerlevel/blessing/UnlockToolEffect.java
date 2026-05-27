package fr.miuby.survi.villager.villagerlevel.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.locked_item.ELockedToolType;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UnlockToolEffect extends BlessingEffect {
    private final ELockedToolType itemType;

    @Override
    public void applyEffect(VillagerLevel villager, AlphaPlayer player) {
        GameManager.getInstance().getLockedItemsFactory().unlockToolItem(player, itemType);
    }
}
