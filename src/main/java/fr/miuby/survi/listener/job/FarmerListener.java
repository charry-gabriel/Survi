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
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Gère les effets du métier {@link EJob#FARMER} :
 * multiplicateur de drops sur les cultures récoltées et sur les mobs passifs tués.
 * La farine d'os est gérée par {@link CropGrowthListener#onFertilize}.
 */
public class FarmerListener implements Listener {

    private static final Set<Material> HARVEST_CROPS = EnumSet.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.NETHER_WART, Material.MELON, Material.PUMPKIN,
            Material.COCOA, Material.CAVE_VINES, Material.CAVE_VINES_PLANT,
            Material.TORCHFLOWER_CROP, Material.PITCHER_CROP, Material.PITCHER_PLANT
    );

    /** Item minimum garanti quand le multiplicateur aboutit à 0 drop — évite de perdre ce qu'on a planté. */
    private static final Map<Material, Material> CROP_SEED = new EnumMap<>(Material.class);
    static {
        CROP_SEED.put(Material.WHEAT,            Material.WHEAT_SEEDS);
        CROP_SEED.put(Material.CARROTS,          Material.CARROT);
        CROP_SEED.put(Material.POTATOES,         Material.POTATO);
        CROP_SEED.put(Material.BEETROOTS,        Material.BEETROOT_SEEDS);
        CROP_SEED.put(Material.NETHER_WART,      Material.NETHER_WART);
        CROP_SEED.put(Material.MELON,            Material.MELON_SLICE);
        CROP_SEED.put(Material.PUMPKIN,          Material.PUMPKIN_SEEDS);
        CROP_SEED.put(Material.COCOA,            Material.COCOA_BEANS);
        CROP_SEED.put(Material.CAVE_VINES,       Material.GLOW_BERRIES);
        CROP_SEED.put(Material.CAVE_VINES_PLANT, Material.GLOW_BERRIES);
        CROP_SEED.put(Material.TORCHFLOWER_CROP, Material.TORCHFLOWER_SEEDS);
        CROP_SEED.put(Material.PITCHER_CROP,     Material.PITCHER_POD);
        CROP_SEED.put(Material.PITCHER_PLANT,    Material.PITCHER_POD);
    }

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
        int level = alpha != null ? alpha.getJobLevel(EJob.FARMER) : 0;
        try (var t = PerfTimer.start("FarmerListener.dropWithMultiplier")) {
            boolean dropped = JobUtils.dropWithMultiplier(event, JobUtils.getMultiplier(EJob.FARMER, level));
            if (!dropped) {
                Material seed = CROP_SEED.get(event.getBlock().getType());
                if (seed != null) event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(seed));
            }
        }
    }

    // ─── Mobs passifs ─────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPassiveMobDeath(EntityDeathEvent event) {
        if (!PASSIVE_MOBS.contains(event.getEntityType())) return;
        Player killer = event.getEntity().getKiller();
        AlphaPlayer alpha = killer != null ? AlphaPlayer.get(killer.getUniqueId()) : null;
        int level = alpha != null ? alpha.getJobLevel(EJob.FARMER) : 0;
        JobUtils.applyDropMultiplier(event.getDrops(), JobUtils.getMultiplier(EJob.FARMER, level));
    }
}