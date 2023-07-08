package fr.miuby.survi.blessing;

import fr.miuby.survi.AlphaPlayer;
import fr.miuby.survi.GameManager;
import org.bukkit.GameRule;

public class RegenEffect extends BlessingEffect {

    @Override
    public void applyEffect(AlphaPlayer player) {
        GameManager.getInstance().GetWorld("Wilderness").setGameRule(GameRule.NATURAL_REGENERATION, true);
    }
}
