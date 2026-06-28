package fr.miuby.survi.listener.job;

import fr.miuby.survi.job.EJob;
import fr.miuby.survi.system.block.MaterialUtils;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.perf.PerfTimer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Gère les effets du métier {@link EJob#FARMER} :
 * multiplicateur de drops sur les cultures récoltées et sur les mobs passifs tués.
 * La farine d'os est gérée par {@link CropGrowthListener#onFertilize}.
 */
public class FarmerListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHarvest(BlockBreakEvent event) {
        if (!MaterialUtils.HARVEST_CROPS.contains(event.getBlock().getType())) return;
        AlphaPlayer alpha = AlphaPlayer.get(event.getPlayer().getUniqueId());
        int level = alpha != null ? alpha.getJobLevel(EJob.FARMER) : 0;
        try (var t = PerfTimer.start("FarmerListener.dropWithMultiplier")) {
            boolean dropped = JobUtils.dropWithMultiplier(event, JobUtils.getMultiplier(EJob.FARMER, level));
            if (!dropped) {
                Material seed = MaterialUtils.CROP_SEED.get(event.getBlock().getType());
                if (seed != null) JobUtils.dropAtBlock(event.getBlock(), new ItemStack(seed));
            }
        }
    }

    // ─── Mobs passifs ─────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPassiveMobDeath(EntityDeathEvent event) {
        if (!MaterialUtils.PASSIVE_MOBS.contains(event.getEntityType())) return;
        Player killer = event.getEntity().getKiller();
        AlphaPlayer alpha = killer != null ? AlphaPlayer.get(killer.getUniqueId()) : null;
        int level = alpha != null ? alpha.getJobLevel(EJob.FARMER) : 0;
        JobUtils.applyDropMultiplier(event.getDrops(), JobUtils.getMultiplier(EJob.FARMER, level));
    }
}