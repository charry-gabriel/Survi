package fr.miuby.survi.listener.job;

import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.perf.PerfTimer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.EnumSet;
import java.util.Set;

/** Multiplicateur de drops sur les minerais selon le niveau {@link EJob#MINER}. */
public class MinerListener implements Listener {

    private static final Set<Material> ORE_BLOCKS = EnumSet.of(
            Material.COAL_ORE,            Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE,            Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE,            Material.DEEPSLATE_GOLD_ORE,  Material.NETHER_GOLD_ORE,
            Material.DIAMOND_ORE,         Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE,         Material.DEEPSLATE_EMERALD_ORE,
            Material.LAPIS_ORE,           Material.DEEPSLATE_LAPIS_ORE,
            Material.REDSTONE_ORE,        Material.DEEPSLATE_REDSTONE_ORE,
            Material.COPPER_ORE,          Material.DEEPSLATE_COPPER_ORE,
            Material.NETHER_QUARTZ_ORE,
            Material.ANCIENT_DEBRIS
    );

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!ORE_BLOCKS.contains(event.getBlock().getType())) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        if (alpha == null) return;

        try (var t = PerfTimer.start("MinerListener.dropWithMultiplier")) {
            JobUtils.dropWithMultiplier(event, JobUtils.getMultiplier(alpha.getJobLevel(EJob.MINER)));
        }
    }
}
