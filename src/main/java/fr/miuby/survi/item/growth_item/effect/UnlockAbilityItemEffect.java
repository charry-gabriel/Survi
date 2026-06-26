package fr.miuby.survi.item.growth_item.effect;

import fr.miuby.survi.item.growth_item.GrowthItems;
import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Débloque une capacité de gameplay (ex. {@code tree_feller}, {@code vein_miner},
 * {@code underwater_kit} — voir les constantes {@code GrowthItems.ABILITY_*}) tant que ce growth
 * item est porté/tenu, indépendamment de toute autre condition.
 *
 * <p>Stocke {@code abilityId} dans un ensemble CSV en PDC ({@link GrowthItems#UNLOCKED_ABILITIES_KEY}).
 * Les listeners de métiers consultent cette capacité via {@link GrowthItems#hasAbilityEquipped} pour
 * activer une mécanique — la <b>magnitude</b> (nombre de blocs, intensité…) reste pilotée par le niveau
 * du job dans {@code JobsConfig}, complètement indépendamment du palier de l'item.</p>
 *
 * <p>Usage YAML (dans un palier) :
 * <pre>
 *   - { type: unlock_ability, value: tree_feller }
 * </pre>
 * </p>
 */
public record UnlockAbilityItemEffect(String abilityId) implements ItemEffect {

    @Override
    public void apply(ItemStack item, AlphaPlayer player) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        Set<String> unlocked = new HashSet<>(Arrays.asList(
                pdc.getOrDefault(GrowthItems.UNLOCKED_ABILITIES_KEY, PersistentDataType.STRING, "").split(",")));
        unlocked.remove("");

        if (unlocked.add(abilityId)) {
            pdc.set(GrowthItems.UNLOCKED_ABILITIES_KEY, PersistentDataType.STRING, String.join(",", unlocked));
            item.setItemMeta(meta);
        }
    }

    @Override
    public boolean isTransient() { return false; }
}
