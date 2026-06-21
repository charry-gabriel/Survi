package fr.miuby.survi.listener.job;

import fr.miuby.survi.job.EJob;
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

    /**
     * Multiplicateur de drops pour un niveau donné, lu depuis {@link JobsConfig}.
     * Seuls MINER, LUMBERJACK et FARMER ont un drop-multiplier ; tout autre métier lève une exception.
     */
    static double getMultiplier(EJob job, int level) {
        JobsConfig cfg = JobsConfig.getInstance();
        return switch (job) {
            case MINER      -> cfg.getMiner().getDropMultiplier()[level];
            case LUMBERJACK -> cfg.getLumberjack().getDropMultiplier()[level];
            case FARMER     -> cfg.getFarmer().getDropMultiplier()[level];
            default -> throw new IllegalArgumentException("Pas de drop-multiplier pour le métier : " + job);
        };
    }

    /**
     * Annule les drops vanilla d'un BlockBreakEvent et les remplace
     * en appliquant le multiplicateur (gestion fractionnaire probabiliste).
     *
     * @return {@code true} si au moins un item a été droppé
     */
    static boolean dropWithMultiplier(BlockBreakEvent event, double multiplier) {
        Block block = event.getBlock();
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        Collection<ItemStack> baseDrops = block.getDrops(tool);
        event.setDropItems(false);

        boolean dropped = false;
        for (ItemStack drop : baseDrops) {
            double totalAmount = drop.getAmount() * multiplier;
            int amount = (int) totalAmount;
            if (RANDOM.nextDouble() < totalAmount - amount) amount++;
            if (amount > 0) {
                ItemStack toDrop = drop.clone();
                toDrop.setAmount(amount);
                block.getWorld().dropItemNaturally(block.getLocation(), toDrop);
                dropped = true;
            }
        }
        return dropped;
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