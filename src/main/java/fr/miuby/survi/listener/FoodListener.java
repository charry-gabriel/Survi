package fr.miuby.survi.listener;

import fr.miuby.lib.MiubyLib;
import fr.miuby.survi.GameManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

/**
 * Délègue à {@link fr.miuby.survi.food.FoodOfTheDayManager} le bonus/malus de saturation
 * et de faim lié à la "nourriture du jour".
 */
public class FoodListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Material food = event.getItem().getType();
        if (!food.isEdible()) return;

        Player player = event.getPlayer();
        float saturationBefore = player.getSaturation();
        int foodLevelBefore = player.getFoodLevel();

        MiubyLib.runLater(() ->
                        GameManager.getInstance().getFoodOfTheDayManager().applyFoodBonus(
                                player, food, saturationBefore, foodLevelBefore),
                1L);
    }
}