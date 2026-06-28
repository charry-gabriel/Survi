package fr.miuby.survi.listener.job;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.item.growth_item.GrowthItems;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.config.JobsConfig;
import fr.miuby.survi.system.block.EOreFamily;
import fr.miuby.survi.system.block.MaterialUtils;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.log.ELogTag;
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
import java.util.logging.Level;

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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!MaterialUtils.ORE_BLOCKS.contains(event.getBlock().getType())) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());
        int level = alpha != null ? alpha.getJobLevel(EJob.MINER) : 0;

        try (var t = PerfTimer.start("MinerListener.dropWithMultiplier")) {
            JobUtils.dropWithMultiplier(event, JobUtils.getMultiplier(EJob.MINER, level));
        }

        JobsConfig.MinerCfg miner = JobsConfig.getInstance().getMiner();
        if (miner.getVeinMinerExtraOres()[level] > 0
                && GrowthItems.hasAbilityEquipped(player, GrowthItems.ABILITY_VEIN_MINER, EquipmentSlot.HEAD)
                && MaterialUtils.PICKAXE_MATERIALS.contains(player.getInventory().getItemInMainHand().getType())) {
            if (player.isSneaking()) {
                MLLogManager.getInstance().log(Level.FINE, ELogTag.JOB,
                        "[Miner] Vein miner ignoré (sneak) pour " + player.getName() + " @ " + event.getBlock().getLocation());
            } else {
                try (var t = PerfTimer.start("MinerListener.veinMiner")) {
                    veinMiner(event.getBlock(), player, level, miner);
                }
            }
        }
    }

    // ─── Vein miner (BFS orthogonal sur le filon connecté) ───────────────────────

    private static void veinMiner(Block origin, Player player, int level, JobsConfig.MinerCfg cfg) {
        int extra = cfg.getVeinMinerExtraOres()[level];
        if (extra <= 0) return;
        EOreFamily family = MaterialUtils.ORE_TO_FAMILY.get(origin.getType());
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
            for (BlockFace face : MaterialUtils.ORTHO_FACES) {
                Block nb = cur.getRelative(face);
                if (!visited.add(nb)) continue;
                if (family != MaterialUtils.ORE_TO_FAMILY.get(nb.getType())) continue;
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
                    JobUtils.dropAtBlock(ore, d);
                }
            }
            player.damageItemStack(EquipmentSlot.HAND, 1);
        }
    }
}