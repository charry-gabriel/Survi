package fr.miuby.survi.item.growth_item.effect;

import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.inventory.ItemStack;

@FunctionalInterface
public interface ItemEffect {
    void apply(ItemStack item, AlphaPlayer player);

    /**
     * Retourne {@code true} si l'effet est transitoire (message, haste, potion) et ne doit pas
     * être rejoué lors d'un {@link fr.miuby.survi.item.growth_item.GrowthItems#reapplyAll}.
     *
     * <p>Les effets persistants ({@code name}, {@code set_attribute}, {@code add_enchantment})
     * retournent {@code false} (défaut) et sont réappliqués à chaque reload.</p>
     */
    default boolean isTransient() { return false; }
}