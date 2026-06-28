package fr.miuby.survi.item.growth_item.effect;

import fr.miuby.survi.item.growth_item.GrowthItems;
import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Marque l'item comme brûlant les ennemis lors d'un coup.
 *
 * <p>Stocke la durée de feu (en secondes) dans la clé PDC {@link GrowthItems#FIRE_SECONDS_KEY}.
 * La mise en feu effective est déclenchée par {@code GrowthItemListener.onEntityDamageByEntity}
 * qui lit cette clé sur l'item en main principale du joueur, ainsi que sur ses jambières
 * équipées (slot LEGS — voir {@code GROWTH_FARMER_LEGGINGS}). Restreint aux mobs passifs
 * ({@code MaterialUtils.PASSIVE_MOBS}).</p>
 *
 * <p>Usage YAML (dans un palier) :
 * <pre>
 *   - { type: fire_enemies, seconds: 5 }
 * </pre>
 * </p>
 */
public record FireEnemiesItemEffect(int seconds) implements ItemEffect {

    @Override
    public void apply(ItemStack item, AlphaPlayer player) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(GrowthItems.FIRE_SECONDS_KEY, PersistentDataType.INTEGER, seconds);
        item.setItemMeta(meta);
    }

    @Override
    public boolean isTransient() { return false; }
}