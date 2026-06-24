package fr.miuby.survi.listener.job;

import fr.miuby.lib.MiubyLib;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.config.JobsConfig;
import fr.miuby.survi.listener.PlacedBlockTracker;
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
 * auto-replant (niv.5+), tree feller, dégâts de feu amplifiés et résistance au feu.
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

    // ─── Blocs ───────────────────────────────────────────────────────────────────

    /** Tous les logs susceptibles de passer dans le système bucheron (naturels + stripped). */
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
            // stripped — traités comme les logs normaux
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

    /** Logs strippables (non-stripped uniquement) — utilisé pour bloquer le stripping avant niv.4. */
    private static final Set<Material> STRIPPABLE_LOG_BLOCKS = EnumSet.of(
            Material.OAK_LOG,      Material.OAK_WOOD,
            Material.SPRUCE_LOG,   Material.SPRUCE_WOOD,
            Material.BIRCH_LOG,    Material.BIRCH_WOOD,
            Material.JUNGLE_LOG,   Material.JUNGLE_WOOD,
            Material.ACACIA_LOG,   Material.ACACIA_WOOD,
            Material.DARK_OAK_LOG, Material.DARK_OAK_WOOD,
            Material.CHERRY_LOG,   Material.CHERRY_WOOD,
            Material.MANGROVE_LOG, Material.MANGROVE_WOOD,
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
        // logs naturels
        m.put(Material.OAK_LOG,             Material.OAK_SAPLING);
        m.put(Material.SPRUCE_LOG,          Material.SPRUCE_SAPLING);
        m.put(Material.BIRCH_LOG,           Material.BIRCH_SAPLING);
        m.put(Material.JUNGLE_LOG,          Material.JUNGLE_SAPLING);
        m.put(Material.ACACIA_LOG,          Material.ACACIA_SAPLING);
        m.put(Material.DARK_OAK_LOG,        Material.DARK_OAK_SAPLING);
        m.put(Material.CHERRY_LOG,          Material.CHERRY_SAPLING);
        m.put(Material.MANGROVE_LOG,        Material.MANGROVE_PROPAGULE);
        m.put(Material.PALE_OAK_LOG,        Material.PALE_OAK_SAPLING);
        // stripped logs — même sapling que leur homologue naturel
        m.put(Material.STRIPPED_OAK_LOG,    Material.OAK_SAPLING);
        m.put(Material.STRIPPED_SPRUCE_LOG, Material.SPRUCE_SAPLING);
        m.put(Material.STRIPPED_BIRCH_LOG,  Material.BIRCH_SAPLING);
        m.put(Material.STRIPPED_JUNGLE_LOG, Material.JUNGLE_SAPLING);
        m.put(Material.STRIPPED_ACACIA_LOG, Material.ACACIA_SAPLING);
        m.put(Material.STRIPPED_DARK_OAK_LOG, Material.DARK_OAK_SAPLING);
        m.put(Material.STRIPPED_CHERRY_LOG, Material.CHERRY_SAPLING);
        m.put(Material.STRIPPED_MANGROVE_LOG, Material.MANGROVE_PROPAGULE);
        m.put(Material.STRIPPED_PALE_OAK_LOG, Material.PALE_OAK_SAPLING);
        LOG_TO_SAPLING = Collections.unmodifiableMap(m);
    }

    private static final BlockFace[] ORTHO_FACES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    // ─── État ────────────────────────────────────────────────────────────────────

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
        if (block == null || !STRIPPABLE_LOG_BLOCKS.contains(block.getType())) return;
        ItemStack item = event.getItem();
        if (item == null || !AXE_MATERIALS.contains(item.getType())) return;
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

        if (LOG_BLOCKS.contains(type)) {
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
            if (level >= 5) {
                autoReplant(block);
                if (lj.getTreeFellerExtraLogs()[level] > 0
                        && AXE_MATERIALS.contains(player.getInventory().getItemInMainHand().getType())) {
                    try (var t = PerfTimer.start("LumberjackListener.treeFeller")) {
                        treeFeller(block, player, level, lj);
                    }
                }
            }
            return;
        }

        if (APPLE_LEAF_BLOCKS.contains(type)) {
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