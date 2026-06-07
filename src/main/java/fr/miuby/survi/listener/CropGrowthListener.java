package fr.miuby.survi.listener;

import fr.miuby.lib.MiubyLib;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.world.crops.PlantedCropUtils;
import fr.miuby.survi.world.crops.PlantedCropsManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.role.ERole;
import fr.miuby.survi.role.Role;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Gère la vitesse de croissance des cultures plantées par des fermiers.
 *
 * <ul>
 *   <li>Cultures <em>non plantées</em> par un fermier → croissance vanilla (aucune restriction).</li>
 *   <li>Cultures plantées par un fermier (niv. 0-4) → croissance ralentie.</li>
 *   <li>Cultures plantées par un fermier (niv. 5) → croissance vanilla.</li>
 *   <li>Cultures plantées par un fermier (niv. 6-10) → croissance accélérée (tick(s) bonus).</li>
 * </ul>
 */
public class CropGrowthListener implements Listener {

    // ─── Tables de croissance par niveau (index = niveau, 0 → 10) ────────────────

    /**
     * Probabilité d'autoriser un tick de croissance (niv. 0-4 uniquement).
     * niv.5 = 1.0 (vanilla), utilisé comme borne dans le handler.
     */
    private static final double[] CROP_GROWTH_ALLOW_CHANCE = {
            0.05,  // niv. 0 — 5%  (20× plus lent que vanilla)
            0.15,  // niv. 1
            0.30,  // niv. 2
            0.50,  // niv. 3
            0.70,  // niv. 4
            1.00,  // niv. 5 — vanilla
            1.00, 1.00, 1.00, 1.00, 1.00
    };

    /**
     * Probabilité d'un tick de croissance BONUS immédiatement après un tick normal (niv. 6-10).
     * niv.10 = toujours 1 bonus + 50 % d'un 2e bonus supplémentaire.
     */
    private static final double[] CROP_EXTRA_GROWTH_CHANCE = {
            0, 0, 0, 0, 0, 0,
            0.20,  // niv. 6
            0.40,  // niv. 7
            0.65,  // niv. 8
            1.00,  // niv. 9
            1.00   // niv. 10 (+ 50 % d'un 3e tick via logique dédiée)
    };

    // ════════════════════════════════════════════════════════════════════════════
    //  Enregistrement de la plantation
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPlant(PlayerInteractEvent event) {
        AlphaPlayer alpha = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (alpha == null) return;
        if (!isValidPlantingAttempt(event, alpha)) return;

        Block targetBlock = GameManager.getInstance().getPlantedCropsManager().getTargetBlockForPlanting(event);
        if (targetBlock != null) {
            int farmLevel = alpha.getJobLevel(EJob.FERMIER);
            GameManager.getInstance().getPlantedCropsManager().addPlantedCrop(targetBlock.getLocation(), farmLevel);
        }
    }

    private static boolean isValidPlantingAttempt(PlayerInteractEvent event, AlphaPlayer alpha) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getItem() == null) return false;
        if (!PlantedCropUtils.isPlantable(event.getItem().getType())) return false;
        for (Role role : alpha.getSubRoles()) {
            if (role.type() == ERole.FERMIER) return true;
        }
        return false;
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Croissance des cultures
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropGrow(BlockGrowEvent event) {
        if (!PlantedCropUtils.isCrop(event.getBlock().getType())) return;

        PlantedCropsManager mgr = GameManager.getInstance().getPlantedCropsManager();
        Integer farmLevel = mgr.getFarmLevel(event.getBlock().getLocation());

        // Culture non plantée par un fermier → vanilla, aucune modification
        if (farmLevel == null) return;

        // Niv. 0-4 : ralentissement → annule le tick avec probabilité (1 - chance)
        if (farmLevel < 5 && Math.random() > CROP_GROWTH_ALLOW_CHANCE[farmLevel]) {
            event.setCancelled(true);
            return;
        }

        // Niv. 6-10 : tick(s) bonus après le tick normal
        if (farmLevel >= 6) {
            scheduleExtraGrowth(event.getBlock(), farmLevel);
        }
    }

    /**
     * Planifie un (ou deux pour niv.10) tick(s) de croissance supplémentaires.
     * N'avance le bloc que s'il est toujours un {@link Ageable} non au max.
     */
    private static void scheduleExtraGrowth(Block block, int farmLevel) {
        if (Math.random() >= CROP_EXTRA_GROWTH_CHANCE[farmLevel]) return;

        Location loc = block.getLocation().clone();
        MiubyLib.runLater(() -> {
            Block target = loc.getBlock();
            if (!(target.getBlockData() instanceof Ageable ageable)) return;
            if (ageable.getAge() >= ageable.getMaximumAge()) return;
            ageable.setAge(ageable.getAge() + 1);
            target.setBlockData(ageable);

            // Niv. 10 seulement : 50 % de chance d'un 3e tick de croissance
            if (farmLevel == 10 && Math.random() < 0.50) {
                MiubyLib.runLater(() -> {
                    Block t2 = loc.getBlock();
                    if (!(t2.getBlockData() instanceof Ageable a2)) return;
                    if (a2.getAge() < a2.getMaximumAge()) {
                        a2.setAge(a2.getAge() + 1);
                        t2.setBlockData(a2);
                    }
                }, 1L);
            }
        }, 1L);
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Bonemeal bloqué sur les cultures
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent event) {
        if (PlantedCropUtils.isCrop(event.getBlock().getType()))
            event.setCancelled(true);
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Nettoyage du registre à la récolte
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (PlantedCropUtils.isCrop(event.getBlock().getType()))
            GameManager.getInstance().getPlantedCropsManager().removePlantedCrop(event.getBlock().getLocation());
    }
}