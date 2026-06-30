package fr.miuby.survi.listener.job;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.listener.PlacedBlockTracker;
import fr.miuby.survi.system.block.MaterialUtils;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.system.perf.PerfTimer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.logging.Level;

/**
 * Gère les effets du métier {@link EJob#MINER} :
 * <ul>
 *   <li>Multiplicateur de drops sur les minerais.</li>
 * </ul>
 *
 * <p>Les blocs posés par les joueurs (détectés via {@link PlacedBlockTracker}) sont exclus
 * du multiplicateur : le drop vanilla exact est conservé (100 %).</p>
 */
public class MinerListener implements Listener {

    private final PlacedBlockTracker placedBlockTracker;

    public MinerListener(PlacedBlockTracker placedBlockTracker) {
        this.placedBlockTracker = placedBlockTracker;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!MaterialUtils.ORE_BLOCKS.contains(event.getBlock().getType())) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        Block block = event.getBlock();

        if (placedBlockTracker.isPlaced(block)) {
            MLLogManager.getInstance().log(Level.FINE, ELogTag.JOB, "[Miner] Bloc posé ignoré (drops vanilla 100%) pour " + player.getName() + " @ " + block.getLocation());
            return;
        }

        AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        int level = alpha != null ? alpha.getJobLevel(EJob.MINER) : 0;

        try (var t = PerfTimer.start("MinerListener.dropWithMultiplier")) {
            JobUtils.dropWithMultiplier(event, JobUtils.getMultiplier(EJob.MINER, level));
        }
    }
}