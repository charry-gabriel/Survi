package fr.miuby.survi.listener;

import fr.miuby.lib.MiubyLib;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.perf.PerfTimer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Gère tous les effets du métier {@link EJob#BUCHERON} :
 * multiplicateur de bûches, charbon bonus, pommes (courbe complète),
 * auto-replant, tree feller, dégâts de feu amplifiés et résistance au feu.
 */
public class BucheronListener implements Listener {

    // ─── Blocs ───────────────────────────────────────────────────────────────────
    private static final Set<Material> LOG_BLOCKS = EnumSet.of(
            Material.OAK_LOG, Material.OAK_WOOD, Material.SPRUCE_LOG, Material.SPRUCE_WOOD,
            Material.BIRCH_LOG, Material.BIRCH_WOOD, Material.JUNGLE_LOG, Material.JUNGLE_WOOD,
            Material.ACACIA_LOG, Material.ACACIA_WOOD, Material.DARK_OAK_LOG, Material.DARK_OAK_WOOD,
            Material.CHERRY_LOG, Material.CHERRY_WOOD, Material.MANGROVE_LOG, Material.MANGROVE_WOOD,
            Material.PALE_OAK_LOG, Material.PALE_OAK_WOOD
    );

    private static final Set<Material> APPLE_LEAF_BLOCKS = EnumSet.of(
            Material.OAK_LEAVES, Material.AZALEA_LEAVES, Material.FLOWERING_AZALEA_LEAVES
    );

    private static final Set<Material> AXE_MATERIALS = EnumSet.of(
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
            Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE
    );

    private static final Set<Material> SOIL_BLOCKS = EnumSet.of(
            Material.GRASS_BLOCK, Material.DIRT, Material.COARSE_DIRT,
            Material.PODZOL, Material.FARMLAND, Material.ROOTED_DIRT,
            Material.MUD, Material.MUDDY_MANGROVE_ROOTS
    );

    private static final Map<Material, Material> LOG_TO_SAPLING;
    static {
        Map<Material, Material> m = new EnumMap<>(Material.class);
        m.put(Material.OAK_LOG, Material.OAK_SAPLING);
        m.put(Material.SPRUCE_LOG, Material.SPRUCE_SAPLING);
        m.put(Material.BIRCH_LOG, Material.BIRCH_SAPLING);
        m.put(Material.JUNGLE_LOG, Material.JUNGLE_SAPLING);
        m.put(Material.ACACIA_LOG, Material.ACACIA_SAPLING);
        m.put(Material.DARK_OAK_LOG, Material.DARK_OAK_SAPLING);
        m.put(Material.CHERRY_LOG, Material.CHERRY_SAPLING);
        m.put(Material.MANGROVE_LOG, Material.MANGROVE_PROPAGULE);
        m.put(Material.PALE_OAK_LOG, Material.PALE_OAK_SAPLING);
        LOG_TO_SAPLING = Collections.unmodifiableMap(m);
    }

    private static final BlockFace[] ORTHO_FACES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    // ─── Tables par niveau ───────────────────────────────────────────────────────
    private static final double[] CHARCOAL_CHANCE =
            { 0, 0, 0, 0.15, 0.20, 0.25, 0.30, 0.35, 0.40, 0.45, 0.50 };

    private static final double[] APPLE_LEAF_CHANCE =
            { 0.001, 0.002, 0.003, 0.005, 0.055, 0.085, 0.115, 0.155, 0.205, 0.275, 0.355 };

    private static final double[] FIRE_DAMAGE_MULT =
            { 2.00, 1.75, 1.50, 1.25, 1.10, 1.00, 1.00, 1.00, 1.00, 1.00, 1.00 };

    private static final int[] TREE_FELLER_EXTRA =
            { 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6 };

    private static final int[] FIRE_RESISTANCE_TICKS =
            { 0, 0, 0, 0, 0, 0, 0, 60, 100, 160, 240 };

    // ════════════════════════════════════════════════════════════════════════════
    //  Breaks de blocs (logs + feuilles)
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        if (alpha == null) return;

        Material type = block.getType();

        if (LOG_BLOCKS.contains(type)) {
            int level = alpha.getJobLevel(EJob.BUCHERON);
            try (var t = PerfTimer.start("BucheronListener.dropWithMultiplier")) {
                JobUtils.dropWithMultiplier(event, JobUtils.getMultiplier(level));
            }
            if (level >= 3) dropBonusCharcoal(block, level);
            if (level >= 5) {
                autoReplant(block);
                if (AXE_MATERIALS.contains(player.getInventory().getItemInMainHand().getType())) {
                    try (var t = PerfTimer.start("BucheronListener.treeFeller")) {
                        treeFeller(block, player, level);
                    }
                }
            }
            return;
        }

        if (APPLE_LEAF_BLOCKS.contains(type)) {
            handleAppleLeaves(event, alpha.getJobLevel(EJob.BUCHERON));
        }
    }

    // ─── Charbon bonus ────────────────────────────────────────────────────────────
    private static void dropBonusCharcoal(Block block, int level) {
        if (JobUtils.RANDOM.nextDouble() < CHARCOAL_CHANCE[level])
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.CHARCOAL));
    }

    // ─── Pommes (courbe complète, remplace la chance vanilla) ─────────────────────
    private static void handleAppleLeaves(BlockBreakEvent event, int level) {
        Block block = event.getBlock();
        Collection<ItemStack> vanilla = block.getDrops(event.getPlayer().getInventory().getItemInMainHand());
        event.setDropItems(false);
        for (ItemStack drop : vanilla) {
            if (drop.getType() == Material.APPLE) continue;
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
        }
        if (JobUtils.RANDOM.nextDouble() < APPLE_LEAF_CHANCE[level])
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.APPLE));
    }

    // ─── Auto-replant ─────────────────────────────────────────────────────────────
    private static void autoReplant(Block brokenLog) {
        Material sapling = LOG_TO_SAPLING.get(brokenLog.getType());
        if (sapling == null) return;
        if (!SOIL_BLOCKS.contains(brokenLog.getRelative(BlockFace.DOWN).getType())) return;
        Location loc = brokenLog.getLocation().clone();
        MiubyLib.runLater(() -> {
            Block t = loc.getBlock();
            if (t.getType() == Material.AIR) t.setType(sapling);
        }, 1L);
    }

    // ─── Tree feller (BFS orthogonal) ────────────────────────────────────────────
    private static void treeFeller(Block origin, Player player, int level) {
        int extra = TREE_FELLER_EXTRA[level];
        if (extra <= 0) return;
        ItemStack tool = player.getInventory().getItemInMainHand();
        double mult = JobUtils.getMultiplier(level);

        List<Block> toBreak = new ArrayList<>(extra);
        Set<Block> visited = new HashSet<>();
        Queue<Block> queue = new ArrayDeque<>();
        visited.add(origin);
        queue.add(origin);

        outer:
        while (!queue.isEmpty()) {
            Block cur = queue.poll();
            for (BlockFace face : ORTHO_FACES) {
                Block nb = cur.getRelative(face);
                if (!visited.add(nb)) continue;
                if (!LOG_BLOCKS.contains(nb.getType())) continue;
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
        int level = alpha.getJobLevel(EJob.BUCHERON);
        if (level < 7 || player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) return;
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.FIRE_RESISTANCE, FIRE_RESISTANCE_TICKS[level], 0, false, false));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFireDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.FIRE && cause != EntityDamageEvent.DamageCause.FIRE_TICK) return;
        AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        if (alpha == null) return;
        int level = alpha.getJobLevel(EJob.BUCHERON);
        if (level >= 5) return;
        event.setDamage(event.getDamage() * FIRE_DAMAGE_MULT[level]);
    }
}