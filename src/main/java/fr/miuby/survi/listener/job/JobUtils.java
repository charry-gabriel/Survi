package fr.miuby.survi.listener.job;

import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * Utilitaires statiques partagés entre les listeners de métiers.
 * Package-private : usage interne uniquement.
 */
final class JobUtils {

    static final Random RANDOM = new Random();

    /** Multiplicateur de drops par niveau (index = niveau, 0 → 10). */
    static final double[] MULTIPLIER_TABLE = {
            0.20,  // niv. 0
            0.50,  // niv. 1
            0.80,  // niv. 2
            1.00,  // niv. 3
            1.10,  // niv. 4
            1.20,  // niv. 5
            1.30,  // niv. 6
            1.40,  // niv. 7
            1.50,  // niv. 8
            1.75,  // niv. 9
            2.00   // niv. 10
    };

    static double getMultiplier(int level) { return MULTIPLIER_TABLE[level]; }

    /**
     * Annule les drops vanilla d'un BlockBreakEvent et les remplace
     * en appliquant le multiplicateur (gestion fractionnaire probabiliste).
     */
    static void dropWithMultiplier(BlockBreakEvent event, double multiplier) {
        Block block = event.getBlock();
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        Collection<ItemStack> baseDrops = block.getDrops(tool);
        event.setDropItems(false);

        for (ItemStack drop : baseDrops) {
            double totalAmount = drop.getAmount() * multiplier;
            int amount = (int) totalAmount;
            if (RANDOM.nextDouble() < totalAmount - amount) amount++;
            if (amount > 0) {
                ItemStack toDrop = drop.clone();
                toDrop.setAmount(amount);
                block.getWorld().dropItemNaturally(block.getLocation(), toDrop);
            }
        }
    }

    /**
     * Modifie en place la liste de drops d'un EntityDeathEvent
     * en appliquant le multiplicateur. Items réduits à 0 → retirés.
     */
    static void applyDropMultiplier(List<ItemStack> drops, double multiplier) {
        if (multiplier == 1.0) return;
        drops.removeIf(item -> {
            if (item == null || item.getType().isAir()) return true;
            double totalAmount = item.getAmount() * multiplier;
            int amount = (int) totalAmount;
            if (RANDOM.nextDouble() < totalAmount - amount) amount++;
            if (amount <= 0) return true;
            item.setAmount(Math.min(amount, item.getMaxStackSize()));
            return false;
        });
    }

    private JobUtils() {}
}