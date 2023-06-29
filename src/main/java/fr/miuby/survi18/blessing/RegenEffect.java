package fr.miuby.survi18.blessing;

import fr.miuby.survi18.AlphaPlayer;
import fr.miuby.survi18.GameManager;
import org.bukkit.GameRule;

public class RegenEffect extends BlessingEffect {

    @Override
    public void applyEffect(AlphaPlayer player) {
        GameManager.getInstance().GetWorld("wilderness").setGameRule(GameRule.NATURAL_REGENERATION, true);
    }
}
