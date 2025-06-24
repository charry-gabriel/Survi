package fr.miuby.survi.villager.blessing;

import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.world.EWorld;
import org.bukkit.GameRule;

public class RegenEffect extends BlessingEffect {

    @Override
    public void applyEffect(AlphaPlayer player) {
        WorldRegistry.get(EWorld.WILDERNESS).getWorld().setGameRule(GameRule.NATURAL_REGENERATION, true);
    }
}
