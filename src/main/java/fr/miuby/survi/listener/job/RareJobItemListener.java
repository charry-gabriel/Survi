package fr.miuby.survi.listener.job;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.rare.RareJobItemService;
import fr.miuby.survi.listener.PlacedBlockTracker;
import fr.miuby.survi.system.block.MaterialUtils;
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
        if (!MaterialUtils.ORE_BLOCKS.contains(event.getBlock().getType())) return;
        // Exclure les minerais replacés manuellement par des joueurs
        if (placedBlockTracker.isPlaced(event.getBlock())) return;
        gm.getRareJobItemService().onJobAction(event.getPlayer(), EJob.MINER);
    }

    // ─── Bûcheron ────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLogBroken(BlockBreakEvent event) {
        if (!MaterialUtils.LOG_BLOCKS.contains(event.getBlock().getType())) return;
        if (placedBlockTracker.isPlaced(event.getBlock())) return;
        gm.getRareJobItemService().onJobAction(event.getPlayer(), EJob.LUMBERJACK);
    }

    // ─── Fermier ─────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCropHarvested(BlockBreakEvent event) {
        if (!MaterialUtils.HARVEST_CROPS.contains(event.getBlock().getType())) return;
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