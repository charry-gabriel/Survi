package fr.miuby.survi.villager.villagerlevel.blessing;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import fr.miuby.survi.system.WorldLevelManager;
import lombok.Getter;

/**
 * Blessing effect that raises the global world level by a fixed amount.
 *
 * <p>Increasing the world level simultaneously:
 * <ul>
 *   <li>Boosts mob rarity weights, making rare classes spawn more often.</li>
 *   <li>Increases the mob stat multiplier (HP, damage).</li>
 *   <li>Shifts quest difficulty distribution toward RARE and LEGENDARY.</li>
 * </ul>
 *
 * <p>Usage in {@link fr.miuby.survi.villager.VillagerFactory}:
 * <pre>
 *   new Blessing(new WorldLevelEffect(1))   // +1 niveau du monde
 *   new Blessing(new WorldLevelEffect(2))   // +2 niveaux du monde
 * </pre>
 *
 * @see WorldLevelManager
 */
public class WorldLevelEffect extends BlessingEffect {

    /** Number of world levels gained when this blessing is applied. */
    @Getter
    private final int levelsGained;

    /**
     * @param levelsGained positive integer; how many levels this blessing adds
     */
    public WorldLevelEffect(int levelsGained) {
        if (levelsGained <= 0)
            throw new IllegalArgumentException("levelsGained must be > 0, got: " + levelsGained);
        this.levelsGained = levelsGained;
    }

    @Override
    public void applyEffect(VillagerLevel villager, AlphaPlayer player) {
        WorldLevelManager.getInstance().increment(levelsGained);

        if (player.getPlayer() != null) {
            player.getPlayer().sendMessage(
                    "§6[Monde] §eLe niveau du monde est passé à §b"
                            + WorldLevelManager.getInstance().getLevel()
                            + "§e. Les monstres et récompenses s'adaptent !"
            );
        }
    }
}