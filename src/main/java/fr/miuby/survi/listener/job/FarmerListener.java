package fr.miuby.survi.listener.job;

import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.perf.PerfTimer;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.EnumSet;
import java.util.Set;

/**
 * Gère les effets du métier {@link EJob#FARMER} :
 * multiplicateur de drops sur les cultures récoltées et sur les mobs passifs tués.
 */
public class FarmerListener implements Listener {

    private static final Set<Material> HARVEST_CROPS = EnumSet.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.NETHER_WART, Material.MELON, Material.PUMPKIN,
            Material.COCOA, Material.CAVE_VINES, Material.CAVE_VINES_PLANT,
            Material.TORCHFLOWER_CROP, Material.PITCHER_CROP, Material.PITCHER_PLANT
    );

    private static final Set<EntityType> PASSIVE_MOBS = EnumSet.of(
            EntityType.BEE, EntityType.DONKEY, EntityType.STRIDER, EntityType.AXOLOTL,
            EntityType.MOOSHROOM, EntityType.CAT, EntityType.PIG, EntityType.HORSE,
            EntityType.GOAT, EntityType.CAMEL, EntityType.FROG, EntityType.HOGLIN,
            EntityType.LLAMA, EntityType.TRADER_LLAMA, EntityType.RABBIT, EntityType.WOLF,
            EntityType.SHEEP, EntityType.MULE, EntityType.OCELOT, EntityType.PANDA,
            EntityType.CHICKEN, EntityType.FOX, EntityType.SNIFFER, EntityType.ARMADILLO,
            EntityType.TURTLE, EntityType.COW
    );

    // ─── Cultures ────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHarvest(BlockBreakEvent event) {
        if (!HARVEST_CROPS.contains(event.getBlock().getType())) return;
        AlphaPlayer alpha = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (alpha == null) return;
        try (var t = PerfTimer.start("FarmerListener.dropWithMultiplier")) {
            JobUtils.dropWithMultiplier(event, JobUtils.getMultiplier(alpha.getJobLevel(EJob.FARMER)));
        }
    }

    // ─── Mobs passifs ─────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPassiveMobDeath(EntityDeathEvent event) {
        if (!PASSIVE_MOBS.contains(event.getEntityType())) return;
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        AlphaPlayer alpha = AlphaPlayer.get(killer.getUniqueId());
        if (alpha == null) return;
        JobUtils.applyDropMultiplier(event.getDrops(), JobUtils.getMultiplier(alpha.getJobLevel(EJob.FARMER)));
    }
}
