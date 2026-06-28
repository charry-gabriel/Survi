package fr.miuby.survi.listener.job;

import fr.miuby.lib.MiubyLib;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.item.growth_item.GrowthItems;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.config.JobsConfig;
import fr.miuby.survi.listener.PlacedBlockTracker;
import fr.miuby.survi.system.block.MaterialUtils;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.system.perf.PerfTimer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.logging.Level;

/**
 * Gère tous les effets du métier {@link EJob#LUMBERJACK} :
 * multiplicateur de bûches, charbon bonus, pommes (courbe complète),
 * auto-replant, tree feller, dégâts de feu amplifiés et résistance au feu.
 *
 * <p><b>Auto-replant</b> et <b>tree feller</b> ne dépendent plus du niveau de job seul : ils
 * nécessitent que le joueur porte un plastron ({@code GROWTH_LUMBERJACK_CHESPLATE}) ayant débloqué
 * l'ability correspondante ({@link GrowthItems#ABILITY_AUTO_REPLANT} / {@link GrowthItems#ABILITY_TREE_FELLER}
 * — paliers de croissance, voir {@code growth_items/growth_lumberjack_chestplate.yml}). Le <b>nombre</b>
 * de bûches supplémentaires cassées par tree feller reste piloté par {@code tree-feller-extra-logs}
 * dans {@link JobsConfig} (niveau du job) — l'item débloque la capacité, le job en augmente la portée.</p>
 *
 * <p>Les blocs posés par les joueurs (détectés via {@link PlacedBlockTracker}) sont exclus
 * du multiplicateur et des bonus : le drop vanilla exact est conservé (100 %).</p>
 *
 * <p>Les stripped logs sont traités à l'identique des logs normaux (multiplicateur, charbon,
 * auto-replant, tree feller). Le stripping par clic droit est bloqué avant le niveau 4.</p>
 *
 * <p>Tous les paramètres numériques sont lus depuis {@link JobsConfig} ({@code jobs/lumberjack.yml}).</p>
 */
public class LumberjackListener implements Listener {

    private final PlacedBlockTracker placedBlockTracker;

    public LumberjackListener(PlacedBlockTracker placedBlockTracker) {
        this.placedBlockTracker = placedBlockTracker;
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Stripping — bloqué avant niveau 4
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler(ignoreCancelled = true)
    public void onStripLog(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || !MaterialUtils.STRIPPABLE_LOG_BLOCKS.contains(block.getType())) return;
        ItemStack item = event.getItem();
        if (item == null || !MaterialUtils.AXE_MATERIALS.contains(item.getType())) return;
        AlphaPlayer alpha = AlphaPlayer.get(event.getPlayer().getUniqueId());
        int level = alpha != null ? alpha.getJobLevel(EJob.LUMBERJACK) : 0;
        if (level >= 4) return;
        event.setUseInteractedBlock(Event.Result.DENY);
        MLLogManager.getInstance().log(Level.FINE, ELogTag.JOB,
                "[Lumberjack] Stripping bloqué pour " + event.getPlayer().getName() + " (niv." + level + " < 4) @ " + block.getLocation());
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Breaks de blocs (logs + feuilles)
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());

        Material type = block.getType();
        JobsConfig.LumberjackCfg lj = JobsConfig.getInstance().getLumberjack();

        if (MaterialUtils.LOG_BLOCKS.contains(type)) {
            // Bloc posé par le joueur : drops vanilla sans multiplicateur ni bonus
            if (placedBlockTracker.isPlaced(block)) {
                MLLogManager.getInstance().log(Level.FINE, ELogTag.JOB,
                        "[Lumberjack] Bloc posé ignoré (drops vanilla 100%) pour " + player.getName() + " @ " + block.getLocation());
                return;
            }
            int level = alpha != null ? alpha.getJobLevel(EJob.LUMBERJACK) : 0;
            try (var t = PerfTimer.start("LumberjackListener.dropWithMultiplier")) {
                JobUtils.dropWithMultiplier(event, JobUtils.getMultiplier(EJob.LUMBERJACK, level));
            }
            if (lj.getCharcoalChance()[level] > 0) dropBonusCharcoal(block, level, lj);
            if (GrowthItems.hasAbilityEquipped(player, GrowthItems.ABILITY_AUTO_REPLANT, EquipmentSlot.CHEST)) {
                autoReplant(block);
            }
            if (lj.getTreeFellerExtraLogs()[level] > 0
                    && GrowthItems.hasAbilityEquipped(player, GrowthItems.ABILITY_TREE_FELLER, EquipmentSlot.CHEST)
                    && MaterialUtils.AXE_MATERIALS.contains(player.getInventory().getItemInMainHand().getType())) {
                if (player.isSneaking()) {
                    MLLogManager.getInstance().log(Level.FINE, ELogTag.JOB,
                            "[Lumberjack] Tree feller ignoré (sneak) pour " + player.getName() + " @ " + block.getLocation());
                } else {
                    try (var t = PerfTimer.start("LumberjackListener.treeFeller")) {
                        treeFeller(block, player, level, lj);
                    }
                }
            }
            return;
        }

        if (MaterialUtils.APPLE_LEAF_BLOCKS.contains(type)) {
            handleAppleLeaves(event, alpha != null ? alpha.getJobLevel(EJob.LUMBERJACK) : 0, lj);
        }
    }

    // ─── Charbon bonus ────────────────────────────────────────────────────────────

    private static void dropBonusCharcoal(Block block, int level, JobsConfig.LumberjackCfg lj) {
        if (JobUtils.RANDOM.nextDouble() < lj.getCharcoalChance()[level])
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.CHARCOAL));
    }

    // ─── Pommes (courbe complète, remplace la chance vanilla) ─────────────────────

    private static void handleAppleLeaves(BlockBreakEvent event, int level, JobsConfig.LumberjackCfg lj) {
        Block block = event.getBlock();
        Collection<ItemStack> vanilla = block.getDrops(event.getPlayer().getInventory().getItemInMainHand());
        event.setDropItems(false);
        for (ItemStack drop : vanilla) {
            if (drop.getType() == Material.APPLE) continue;
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
        }
        if (JobUtils.RANDOM.nextDouble() < lj.getAppleLeafChance()[level])
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.APPLE));
    }

    // ─── Auto-replant ─────────────────────────────────────────────────────────────

    private static void autoReplant(Block brokenLog) {
        Material sapling = MaterialUtils.LOG_TO_SAPLING.get(brokenLog.getType());
        if (sapling == null) return;
        if (!MaterialUtils.SOIL_BLOCKS.contains(brokenLog.getRelative(BlockFace.DOWN).getType())) return;
        Location loc = brokenLog.getLocation().clone();
        MiubyLib.runLater(() -> {
            Block t = loc.getBlock();
            if (t.getType() == Material.AIR) t.setType(sapling);
        }, 1L);
    }

    // ─── Tree feller (BFS orthogonal) ────────────────────────────────────────────

    private static void treeFeller(Block origin, Player player, int level, JobsConfig.LumberjackCfg lj) {
        int extra = lj.getTreeFellerExtraLogs()[level];
        if (extra <= 0) return;
        ItemStack tool = player.getInventory().getItemInMainHand();
        double mult = JobUtils.getMultiplier(EJob.LUMBERJACK, level);

        List<Block> toBreak = new ArrayList<>(extra);
        Set<Block> visited = new HashSet<>();
        Queue<Block> queue = new ArrayDeque<>();
        visited.add(origin);
        queue.add(origin);

        outer:
        while (!queue.isEmpty()) {
            Block cur = queue.poll();
            for (BlockFace face : MaterialUtils.ORTHO_FACES) {
                Block nb = cur.getRelative(face);
                if (!visited.add(nb)) continue;
                if (!MaterialUtils.LOG_BLOCKS.contains(nb.getType())) continue;
                toBreak.add(nb);
                if (toBreak.size() >= extra) break outer;
                queue.add(nb);
            }
        }

        for (Block log : toBreak) {
            Collection<ItemStack> drops = log.getDrops(tool);
            log.setType(Material.AIR);
            for (ItemStack drop : drops) {
                double total = drop.getAmount() * mult;
                int amt = (int) total;
                if (JobUtils.RANDOM.nextDouble() < total - amt) amt++;
                if (amt > 0) {
                    ItemStack d = drop.clone(); d.setAmount(amt);
                    log.getWorld().dropItemNaturally(log.getLocation(), d);
                }
            }
            player.damageItemStack(EquipmentSlot.HAND, 1);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Feu
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCombust(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        if (alpha == null) return;
        int level = alpha.getJobLevel(EJob.LUMBERJACK);
        JobsConfig.LumberjackCfg lj = JobsConfig.getInstance().getLumberjack();
        int resTicks = lj.getFireResistanceTicks()[level];
        if (resTicks <= 0 || player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) return;
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, resTicks, 0, false, false));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFireDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.FIRE && cause != EntityDamageEvent.DamageCause.FIRE_TICK) return;
        AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        if (alpha == null) return;
        int level = alpha.getJobLevel(EJob.LUMBERJACK);
        double mult = JobsConfig.getInstance().getLumberjack().getFireDamageMultiplier()[level];
        if (mult >= 1.0) return;
        event.setDamage(event.getDamage() * mult);
    }
}