package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.AlphaPlayer;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.GameManager;
import org.bukkit.GameRule;

public class RegenEffect extends BlessingEffect {

    @Override
    public void applyEffect(AlphaPlayer player) {
        GameManager.getInstance().getWorld(EWorld.WILDERNESS).setGameRule(GameRule.NATURAL_REGENERATION, true);
    }
}
