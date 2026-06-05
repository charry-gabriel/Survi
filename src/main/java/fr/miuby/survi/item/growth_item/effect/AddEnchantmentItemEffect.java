package fr.miuby.survi.item.growth_item.effect;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.growth_item.GrowthItems;
import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Ajoute N niveaux d'enchantement à l'item à chaque montée de palier.
 *
 * <p>L'application est <strong>idempotente</strong> : la quantité cumulée ajoutée par le système
 * de growth est tracée dans la clé PDC {@code growth_ench_<enchantment>} (INTEGER) portée par
 * l'item lui-même. Cela permet à {@link GrowthItems#reapplyAll}
 * de défaire précisément la contribution du growth avant de réappliquer depuis la nouvelle config,
 * sans toucher aux enchantements posés par le joueur.
 *
 * <p>Exemple — pioche fortune : tier 1 (+1), tier 2 (+1), tier 3 (+2).
 * Après les 3 paliers le PDC contient {@code growth_ench_fortune = 4} et le niveau fortune
 * de l'item vaut {@code base + 4}.
 *
 * @param enchantment Accesseurs utilisés par GrowthItems.resetGrowthEnchantments (inférence legacy)
 */
public record AddEnchantmentItemEffect(Enchantment enchantment, int amount) implements ItemEffect {

    @Override
    public void apply(ItemStack item, AlphaPlayer player) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        NamespacedKey trackKey = new NamespacedKey(GameManager.getInstance().getPlugin(), "growth_ench_" + enchantment.getKey().getKey());
        int previousGrowthAmount = pdc.getOrDefault(trackKey, PersistentDataType.INTEGER, 0);
        int currentLevel = meta.hasEnchant(enchantment) ? meta.getEnchantLevel(enchantment) : 0;
        int baseLevel = Math.max(0, currentLevel - previousGrowthAmount);
        int newCumulative = previousGrowthAmount + amount;

        pdc.set(trackKey, PersistentDataType.INTEGER, newCumulative);
        meta.addEnchant(enchantment, baseLevel + newCumulative, true);
        item.setItemMeta(meta);
    }
}