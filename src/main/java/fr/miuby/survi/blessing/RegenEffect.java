package fr.miuby.survi.blessing;

import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.world.EWorld;
import org.bukkit.GameRules;

public class RegenEffect extends BlessingEffect {

    @Override
    public void applyEffect(AlphaPlayer player) {
        WorldRegistry.get(EWorld.WILDERNESS).getWorld().setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, true);
    }

    @Override
    public void resetEffect(AlphaPlayer player) {
        WorldRegistry.get(EWorld.WILDERNESS).getWorld().setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, false);
    }
}