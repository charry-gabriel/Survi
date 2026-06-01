package fr.miuby.survi.listener;

import fr.miuby.survi.item.growth_item.GrowthItems;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GrowthItemListener implements Listener {

    private static final Random RANDOM = new Random();

    // ─── Blocs minerais — grandit le GROWTH_CASQUE_MINEUR ────────────────────────
    private static final Set<Material> ORE_BLOCKS = EnumSet.of(
            Material.COAL_ORE,            Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE,            Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE,            Material.DEEPSLATE_GOLD_ORE,            Material.NETHER_GOLD_ORE,
            Material.DIAMOND_ORE,         Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE,         Material.DEEPSLATE_EMERALD_ORE,
            Material.LAPIS_ORE,           Material.DEEPSLATE_LAPIS_ORE,
            Material.REDSTONE_ORE,        Material.DEEPSLATE_REDSTONE_ORE,
            Material.COPPER_ORE,          Material.DEEPSLATE_COPPER_ORE,
            Material.NETHER_QUARTZ_ORE,
            Material.ANCIENT_DEBRIS
    );

    // ─── Blocs cultures — grandit le GROWTH_BATON_FERMIER ────────────────────────
    // Seules les cultures matures droppent quelque chose en vanilla, donc le bonus
    // s'applique naturellement sans avoir besoin de vérifier le stade de croissance.
    private static final Set<Material> CROP_BLOCKS = EnumSet.of(
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS,
            Material.NETHER_WART,
            Material.SWEET_BERRY_BUSH,
            Material.MELON,
            Material.PUMPKIN,
            Material.COCOA,
            Material.PITCHER_CROP,
            Material.TORCHFLOWER_CROP
    );

    // ─────────────────────────────────────────────────────────────────────────────
    //  Growth items en main (GROWTH_PICKAXE, GROWTH_SWORD, GROWTH_BATON_FERMIER)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Incrémente les uses du growth item tenu en main sur n'importe quel BlockBreakEvent.
     * Cible GROWTH_PICKAXE et GROWTH_SWORD (eventType = "BlockBreakEvent").
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        GrowthItems.IncrementUses(event.getPlayer(), "BlockBreakEvent");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  GROWTH_CASQUE_MINEUR — croît uniquement sur les minerais
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Incrémente les uses du GROWTH_CASQUE_MINEUR (porté en slot HEAD)
     * uniquement lorsque le bloc cassé est un minerai.
     *
     * <p>Priorité NORMAL pour ne pas interférer avec la gestion des drops
     * de {@code JobListener} (priorité HIGH).
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onOreBreak(BlockBreakEvent event) {
        if (!ORE_BLOCKS.contains(event.getBlock().getType())) return;
        GrowthItems.IncrementUsesFromHelmet(event.getPlayer(), "OreBreakEvent");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  GROWTH_BATON_FERMIER — croît sur les cultures + bonus de récolte
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Gère le GROWTH_BATON_FERMIER lors de la destruction d'une culture.
     *
     * <ol>
     *   <li>Si le joueur tient le bâton en main (principale ou secondaire), applique
     *       un bonus de drops proportionnel au tier du bâton.</li>
     *   <li>Incrémente ensuite les uses du bâton (via {@link GrowthItems#IncrementUses}).</li>
     * </ol>
     *
     * <p>Le bonus de drops est ajouté sur les drops vanilla (sans les annuler) :
     * <ul>
     *   <li>Tier 0 → pas de bonus</li>
     *   <li>Tier 1 → ×1,5 (50 % de chance d'un item supplémentaire par drop)</li>
     *   <li>Tier 2 → ×2,0 (double systématique)</li>
     *   <li>Tier 3 → ×2,5 (double + 50 % chance d'un troisième)</li>
     * </ul>
     *
     * <p>Priorité HIGH car les drops sont calculés à ce stade ; la logique de bonus
     * doit être posée avant que le bloc ne soit réellement cassé.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!CROP_BLOCKS.contains(block.getType())) return;

        Player player = event.getPlayer();

        // ── Bonus de drops si le joueur porte le bâton ───────────────────────────
        ItemStack baton = GrowthItems.findGrowthItemInHands(player, "GROWTH_BATON_FERMIER");
        if (baton != null) {
            int tier = baton.getItemMeta().getPersistentDataContainer()
                    .getOrDefault(GrowthItems.TIER_KEY, PersistentDataType.INTEGER, 0);

            if (tier > 0) {
                applyBatonCropBonus(event, tier);
            }
        }

        // ── Incrément des uses du bâton ───────────────────────────────────────────
        GrowthItems.IncrementUses(player, "CropBreakEvent");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  Logique de bonus — séparée pour la lisibilité
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Calcule les drops vanilla et dépose des items bonus dans le monde.
     *
     * <p>Le multiplicateur de bonus est : {@code tier × 0.5} (au-delà de 1.0 de base).
     * Exemple : tier 2 → multiplicateur bonus = 1.0 → chaque item vanilla est dupliqué une fois.
     *
     * <p>Les drops vanilla continuent de tomber normalement (on ne les annule pas).
     * Les drops bonus sont déposés séparément via {@code dropItemNaturally}.
     */
    private void applyBatonCropBonus(BlockBreakEvent event, int tier) {
        Block block = event.getBlock();
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();

        // getDrops() est un calcul sans effet de bord — les drops vanilla tombent indépendamment
        Collection<ItemStack> vanillaDrops = block.getDrops(tool);
        if (vanillaDrops.isEmpty()) return;

        double bonusMultiplier = tier * 0.5; // tier 1 → +50%, tier 2 → +100%, tier 3 → +150%

        List<ItemStack> bonusDrops = new ArrayList<>();
        for (ItemStack drop : vanillaDrops) {
            double totalBonus = drop.getAmount() * bonusMultiplier;
            int bonus = (int) totalBonus;
            double fractional = totalBonus - bonus;
            if (fractional > 0 && RANDOM.nextDouble() < fractional) bonus++;
            if (bonus > 0) {
                ItemStack bonusDrop = drop.clone();
                bonusDrop.setAmount(bonus);
                bonusDrops.add(bonusDrop);
            }
        }

        for (ItemStack bonusDrop : bonusDrops) {
            block.getWorld().dropItemNaturally(block.getLocation(), bonusDrop);
        }
    }
}