package fr.miuby.survi.job.alchemic;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Entrée de la table de loot alchimique du Pêcheur.
 *
 * <p>Trois sous-types :</p>
 * <ul>
 *   <li>{@link IngredientEntry} — ingrédient de brassage (drop simple).</li>
 *   <li>{@link VanillaPotionEntry} — potion vanilla à effets nerfés.</li>
 *   <li>{@link CustomPotionEntry} — potion inédite ({@link ECustomPotion}).</li>
 * </ul>
 *
 * <p>Chaque entrée porte un {@code levelMin} (niveau minimum du pêcheur pour pouvoir pêcher cet item)
 * et un {@code weight} (poids dans le tirage au sort parmi les entrées éligibles).</p>
 */
public sealed interface AlchemicLootEntry
        permits AlchemicLootEntry.IngredientEntry,
                AlchemicLootEntry.VanillaPotionEntry,
                AlchemicLootEntry.CustomPotionEntry {

    int levelMin();
    int weight();

    /** Crée l'ItemStack correspondant à ce loot. */
    ItemStack createItem();

    // ─── Ingrédient ──────────────────────────────────────────────────────────────

    record IngredientEntry(
            Material material,
            int levelMin,
            int weight
    ) implements AlchemicLootEntry {
        @Override public ItemStack createItem() { return new ItemStack(material); }
    }

    // ─── Potion vanilla (nerfée) ─────────────────────────────────────────────────

    record VanillaPotionEntry(
            PotionEffectType effect,
            int duration,
            int amplifier,
            boolean splash,
            int levelMin,
            int weight
    ) implements AlchemicLootEntry {
        @Override
        public ItemStack createItem() {
            Material mat = splash
                    ? Material.SPLASH_POTION
                    : Material.POTION;
            ItemStack item = new ItemStack(mat);
            PotionMeta meta = (PotionMeta) item.getItemMeta();
            meta.addCustomEffect(new PotionEffect(effect, duration, amplifier, false, true), true);
            item.setItemMeta(meta);
            return item;
        }
    }

    // ─── Potion custom inédite ────────────────────────────────────────────────────

    record CustomPotionEntry(
            ECustomPotion potion,
            int levelMin,
            int weight
    ) implements AlchemicLootEntry {
        @Override public ItemStack createItem() { return potion.createItem(); }
    }
}
