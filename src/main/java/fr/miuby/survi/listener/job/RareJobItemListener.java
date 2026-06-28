package fr.miuby.survi.listener.job;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.rare.RareItemConfig;
import fr.miuby.survi.job.rare.RareJobItemService;
import fr.miuby.survi.listener.PlacedBlockTracker;
import fr.miuby.survi.system.block.MaterialUtils;
import org.bukkit.block.Block;
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
 *   <li><b>Explorateur</b> — monstre tué à une distance configurable du 0,0 (EntityDeathEvent)</li>
 * </ul>
 *
 * La distance minimale de l'Explorateur est lue depuis {@link RareItemConfig} et mise à jour
 * à chaque reload sans redémarrage.
 */
public class RareJobItemListener implements Listener {

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
        Block block = event.getBlock();
        if (!MaterialUtils.isLegitimateMineBreak(block, placedBlockTracker.isPlaced(block))) return;
        gm.getRareJobItemService().onJobAction(event.getPlayer(), EJob.MINER);
    }

    // ─── Bûcheron ────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLogBroken(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!MaterialUtils.isLegitimateLumberBreak(block, placedBlockTracker.isPlaced(block))) return;
        gm.getRareJobItemService().onJobAction(event.getPlayer(), EJob.LUMBERJACK);
    }

    // ─── Fermier ─────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCropHarvested(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!MaterialUtils.isLegitimateHarvest(block, placedBlockTracker.isPlaced(block))) return;
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

    // ─── Explorateur — monstre tué à la distance configurée du 0,0 ──────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobKill(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Mob)) return;
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        double x = killer.getLocation().getX();
        double z = killer.getLocation().getZ();
        if (x * x + z * z < RareItemConfig.getInstance().getExplorerMinDistanceSq()) return;

        gm.getRareJobItemService().onJobAction(killer, EJob.EXPLORER);
    }
}