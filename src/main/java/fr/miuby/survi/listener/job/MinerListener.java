package fr.miuby.survi.listener.job;

import fr.miuby.survi.item.growth_item.GrowthItems;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.config.JobsConfig;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.perf.PerfTimer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Gère les effets du métier {@link EJob#MINER} :
 * <ul>
 *   <li>Multiplicateur de drops sur les minerais.</li>
 *   <li><b>Vein miner</b> — casse le filon connecté (BFS orthogonal), débloqué par l'ability
 *       {@link GrowthItems#ABILITY_VEIN_MINER} du casque ({@code GROWTH_MINER_HELMET} — voir
 *       {@code growth_items/growth_miner_helmet.yml}). Le <b>nombre</b> de minerais supplémentaires
 *       reste piloté par {@code vein-miner-extra-ores} dans {@link JobsConfig} (niveau du job) :
 *       l'item débloque la capacité, le job en augmente la portée — même principe que le tree feller
 *       du Bûcheron ({@link LumberjackListener}).</li>
 * </ul>
 */
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

    private static final Set<Material> PICKAXE_MATERIALS = EnumSet.of(
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE
    );

    /**
     * Regroupe chaque variant deepslate avec son équivalent pierre (ex. {@code DEEPSLATE_IRON_ORE} ↔
     * {@code IRON_ORE}) : un filon traversant la transition pierre/deepslate reste un seul filon pour
     * le vein miner. {@code NETHER_GOLD_ORE}, {@code NETHER_QUARTZ_ORE} et {@code ANCIENT_DEBRIS}
     * forment chacun leur propre famille (pas d'équivalent deepslate).
     */
    private static final Map<Material, Material> ORE_FAMILY;
    static {
        Map<Material, Material> m = new EnumMap<>(Material.class);
        m.put(Material.COAL_ORE,              Material.COAL_ORE);
        m.put(Material.DEEPSLATE_COAL_ORE,     Material.COAL_ORE);
        m.put(Material.IRON_ORE,               Material.IRON_ORE);
        m.put(Material.DEEPSLATE_IRON_ORE,     Material.IRON_ORE);
        m.put(Material.GOLD_ORE,               Material.GOLD_ORE);
        m.put(Material.DEEPSLATE_GOLD_ORE,     Material.GOLD_ORE);
        m.put(Material.NETHER_GOLD_ORE,        Material.NETHER_GOLD_ORE);
        m.put(Material.DIAMOND_ORE,            Material.DIAMOND_ORE);
        m.put(Material.DEEPSLATE_DIAMOND_ORE,  Material.DIAMOND_ORE);
        m.put(Material.EMERALD_ORE,            Material.EMERALD_ORE);
        m.put(Material.DEEPSLATE_EMERALD_ORE,  Material.EMERALD_ORE);
        m.put(Material.LAPIS_ORE,              Material.LAPIS_ORE);
        m.put(Material.DEEPSLATE_LAPIS_ORE,    Material.LAPIS_ORE);
        m.put(Material.REDSTONE_ORE,           Material.REDSTONE_ORE);
        m.put(Material.DEEPSLATE_REDSTONE_ORE, Material.REDSTONE_ORE);
        m.put(Material.COPPER_ORE,             Material.COPPER_ORE);
        m.put(Material.DEEPSLATE_COPPER_ORE,   Material.COPPER_ORE);
        m.put(Material.NETHER_QUARTZ_ORE,      Material.NETHER_QUARTZ_ORE);
        m.put(Material.ANCIENT_DEBRIS,         Material.ANCIENT_DEBRIS);
        ORE_FAMILY = Collections.unmodifiableMap(m);
    }

    private static final BlockFace[] ORTHO_FACES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!ORE_BLOCKS.contains(event.getBlock().getType())) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        int level = alpha != null ? alpha.getJobLevel(EJob.MINER) : 0;

        try (var t = PerfTimer.start("MinerListener.dropWithMultiplier")) {
            JobUtils.dropWithMultiplier(event, JobUtils.getMultiplier(EJob.MINER, level));
        }

        JobsConfig.MinerCfg miner = JobsConfig.getInstance().getMiner();
        if (miner.getVeinMinerExtraOres()[level] > 0
                && GrowthItems.hasAbilityEquipped(player, GrowthItems.ABILITY_VEIN_MINER, EquipmentSlot.HEAD)
                && PICKAXE_MATERIALS.contains(player.getInventory().getItemInMainHand().getType())) {
            try (var t = PerfTimer.start("MinerListener.veinMiner")) {
                veinMiner(event.getBlock(), player, level, miner);
            }
        }
    }

    // ─── Vein miner (BFS orthogonal sur le filon connecté) ───────────────────────

    private static void veinMiner(Block origin, Player player, int level, JobsConfig.MinerCfg cfg) {
        int extra = cfg.getVeinMinerExtraOres()[level];
        if (extra <= 0) return;
        Material family = ORE_FAMILY.get(origin.getType());
        if (family == null) return;
        ItemStack tool = player.getInventory().getItemInMainHand();
        double mult = JobUtils.getMultiplier(EJob.MINER, level);

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
                if (!family.equals(ORE_FAMILY.get(nb.getType()))) continue;
                toBreak.add(nb);
                if (toBreak.size() >= extra) break outer;
                queue.add(nb);
            }
        }

        for (Block ore : toBreak) {
            Collection<ItemStack> drops = ore.getDrops(tool);
            ore.setType(Material.AIR);
            for (ItemStack drop : drops) {
                double total = drop.getAmount() * mult;
                int amt = (int) total;
                if (JobUtils.RANDOM.nextDouble() < total - amt) amt++;
                if (amt > 0) {
                    ItemStack d = drop.clone(); d.setAmount(amt);
                    ore.getWorld().dropItemNaturally(ore.getLocation(), d);
                }
            }
            player.damageItemStack(EquipmentSlot.HAND, 1);
        }
    }
}
