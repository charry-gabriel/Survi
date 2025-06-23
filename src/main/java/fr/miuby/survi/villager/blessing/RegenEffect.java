package fr.miuby.survi.villager.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.WorldFactory;
import org.bukkit.GameRule;

public class RegenEffect extends BlessingEffect {

    @Override
    public void applyEffect(AlphaPlayer player) {
        WorldFactory.get(EWorld.WILDERNESS).getWorld().setGameRule(GameRule.NATURAL_REGENERATION, true);
    }
}
