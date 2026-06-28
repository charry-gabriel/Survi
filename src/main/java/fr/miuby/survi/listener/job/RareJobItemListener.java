package fr.miuby.survi.listener.job;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.rare.RareJobItemService;
import fr.miuby.survi.listener.PlacedBlockTracker;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.EnumSet;
import java.util.Set;

/**
 * Écoute les events liés aux métiers pour alimenter le système d'objets rares de collection.
 *
 * <p>Chaque action qualifiante délègue à {@link RareJobItemService#onJobAction}.</p>
 *
 * <ul>
 *   <li><b>Mineur</b>   — cassage de minerai (BlockBreakEvent)</li>
 *   <li><b>Bûcheron</b> — cassage de bûche (BlockBreakEvent)</li>
 *   <li><b>Fermier</b>  — récolte de culture (BlockBreakEvent)</li>
 *   <li><b>Enchanteur</b> — enchantement appliqué (EnchantItemEvent)</li>
 *   <li><b>Pêcheur</b>  — poisson attrapé (PlayerFishEvent CAUGHT_FISH)</li>
 *   <li><b>Explorateur</b> — monstre tué à plus de 600 blocs du 0,0 (EntityDeathEvent)</li>
 * </ul>
 */
public class RareJobItemListener implements Listener {

    // ─── Ensembles de blocs (répliqués ici car private dans leurs listeners d'origine) ──

    private static final Set<Material> ORE_BLOCKS = EnumSet.of(
            Material.COAL_ORE,            Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE,            Material.DEEPSLATE_IRON_ORE,
            Material.GOLD_ORE,            Material.DEEPSLATE_GOLD_ORE,   Material.NETHER_GOLD_ORE,
            Material.DIAMOND_ORE,         Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE,         Material.DEEPSLATE_EMERALD_ORE,
            Material.LAPIS_ORE,           Material.DEEPSLATE_LAPIS_ORE,
            Material.REDSTONE_ORE,        Material.DEEPSLATE_REDSTONE_ORE,
            Material.COPPER_ORE,          Material.DEEPSLATE_COPPER_ORE,
            Material.NETHER_QUARTZ_ORE,
            Material.ANCIENT_DEBRIS
    );

    private static final Set<Material> LOG_BLOCKS = EnumSet.of(
            Material.OAK_LOG,               Material.OAK_WOOD,
            Material.SPRUCE_LOG,            Material.SPRUCE_WOOD,
            Material.BIRCH_LOG,             Material.BIRCH_WOOD,
            Material.JUNGLE_LOG,            Material.JUNGLE_WOOD,
            Material.ACACIA_LOG,            Material.ACACIA_WOOD,
            Material.DARK_OAK_LOG,          Material.DARK_OAK_WOOD,
            Material.CHERRY_LOG,            Material.CHERRY_WOOD,
            Material.MANGROVE_LOG,          Material.MANGROVE_WOOD,
            Material.PALE_OAK_LOG,          Material.PALE_OAK_WOOD,
            Material.STRIPPED_OAK_LOG,      Material.STRIPPED_OAK_WOOD,
            Material.STRIPPED_SPRUCE_LOG,   Material.STRIPPED_SPRUCE_WOOD,
            Material.STRIPPED_BIRCH_LOG,    Material.STRIPPED_BIRCH_WOOD,
            Material.STRIPPED_JUNGLE_LOG,   Material.STRIPPED_JUNGLE_WOOD,
            Material.STRIPPED_ACACIA_LOG,   Material.STRIPPED_ACACIA_WOOD,
            Material.STRIPPED_DARK_OAK_LOG, Material.STRIPPED_DARK_OAK_WOOD,
            Material.STRIPPED_CHERRY_LOG,   Material.STRIPPED_CHERRY_WOOD,
            Material.STRIPPED_MANGROVE_LOG, Material.STRIPPED_MANGROVE_WOOD,
            Material.STRIPPED_PALE_OAK_LOG, Material.STRIPPED_PALE_OAK_WOOD
    );

    private static final Set<Material> HARVEST_CROPS = EnumSet.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.NETHER_WART, Material.MELON, Material.PUMPKIN,
            Material.COCOA, Material.CAVE_VINES, Material.CAVE_VINES_PLANT,
            Material.TORCHFLOWER_CROP, Material.PITCHER_CROP, Material.PITCHER_PLANT
    );

    /** Distance horizontale au carré depuis laquelle l'Explorateur peut obtenir son objet. */
    private static final double EXPLORER_DIST_SQ = 600.0 * 600.0;

    // ─── Références ──────────────────────────────────────────────────────────────

    private final GameManager gm;
    private final PlacedBlockTracker placedBlockTracker;

    /**
     * @param placedBlockTracker tracker de blocs posés par des joueurs,
     *                           utilisé pour exclure les blocs artificiels
     *                           du comptage Mineur et Fermier.
     */
    public RareJobItemListener(PlacedBlockTracker placedBlockTracker) {
        this.gm = GameManager.getInstance();
        this.placedBlockTracker = placedBlockTracker;
    }

    // ─── Cycle de vie joueur ─────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        RareJobItemService svc = gm.getRareJobItemService();
        if (svc != null) svc.loadPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        RareJobItemService svc = gm.getRareJobItemService();
        if (svc != null) svc.unloadPlayer(event.getPlayer().getUniqueId());
    }

    // ─── Mineur ──────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOreBroken(BlockBreakEvent event) {
        if (!ORE_BLOCKS.contains(event.getBlock().getType())) return;
        // Exclure les minerais replacés manuellement par des joueurs
        if (placedBlockTracker.isPlaced(event.getBlock())) return;
        gm.getRareJobItemService().onJobAction(event.getPlayer(), EJob.MINER);
    }

    // ─── Bûcheron ────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLogBroken(BlockBreakEvent event) {
        if (!LOG_BLOCKS.contains(event.getBlock().getType())) return;
        if (placedBlockTracker.isPlaced(event.getBlock())) return;
        gm.getRareJobItemService().onJobAction(event.getPlayer(), EJob.LUMBERJACK);
    }

    // ─── Fermier ─────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCropHarvested(BlockBreakEvent event) {
        if (!HARVEST_CROPS.contains(event.getBlock().getType())) return;
        // Exclure les cultures re-plantées artificiellement sur des blocs tracés
        if (placedBlockTracker.isPlaced(event.getBlock())) return;
        gm.getRareJobItemService().onJobAction(event.getPlayer(), EJob.FARMER);
    }

    // ─── Enchanteur ──────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        gm.getRareJobItemService().onJobAction(event.getEnchanter(), EJob.ENCHANTER);
    }

    // ─── Pêcheur ─────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        gm.getRareJobItemService().onJobAction(event.getPlayer(), EJob.FISHERMAN);
    }

    // ─── Explorateur — monstre tué à plus de 600 blocs du 0,0 ───────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobKill(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Mob)) return;
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        double x = killer.getLocation().getX();
        double z = killer.getLocation().getZ();
        if (x * x + z * z < EXPLORER_DIST_SQ) return;

        gm.getRareJobItemService().onJobAction(killer, EJob.EXPLORER);
    }
}