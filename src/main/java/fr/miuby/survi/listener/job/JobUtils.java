package fr.miuby.survi.listener.job;

import fr.miuby.survi.job.config.JobsConfig;
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

    /** Multiplicateur de drops pour un niveau donné, lu depuis {@link JobsConfig}. */
    static double getMultiplier(int level) {
        return JobsConfig.getInstance().getDropMultiplier()[level];
    }

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
