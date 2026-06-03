package fr.miuby.survi.blessing;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Blessing effect that raises the global world level by exactly one.
 *
 * @see fr.miuby.survi.world.WorldLevelManager
 */
public class WorldLevelEffect extends BlessingEffect {

    @Override
    public void applyEffect(AlphaPlayer player) {
        GameManager.getInstance().getWorldLevelManager().increment();

        if (player.getPlayer() != null) {
            player.getPlayer().sendMessage(
                    Component.text("[Monde] ", NamedTextColor.GOLD)
                            .append(Component.text("Le niveau du monde est passé à ", NamedTextColor.YELLOW))
                            .append(Component.text(String.valueOf(GameManager.getInstance().getWorldLevelManager().getLevel()), NamedTextColor.AQUA))
                            .append(Component.text(". Les monstres et récompenses s'adaptent !", NamedTextColor.YELLOW))
            );
        }
    }

    @Override
    public void resetEffect(AlphaPlayer player) {
        GameManager.getInstance().getWorldLevelManager().decrement();
    }
}