package fr.miuby.survi.listener.job;

import fr.miuby.lib.MiubyLib;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.config.JobsConfig;
import fr.miuby.survi.world.crops.PlantedCropUtils;
import fr.miuby.survi.world.crops.PlantedCropsManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.log.ELogTag;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Level;

/**
 * Gère la vitesse de croissance des cultures plantées par des fermiers.
 *
 * <ul>
 *   <li>Cultures <em>non plantées</em> par un fermier → croissance vanilla (aucune restriction).</li>
 *   <li>Cultures plantées par un fermier (niv. 0-4) → croissance ralentie.</li>
 *   <li>Cultures plantées par un fermier (niv. 5) → croissance vanilla.</li>
 *   <li>Cultures plantées par un fermier (niv. 6-10) → croissance accélérée (tick(s) bonus).</li>
 * </ul>
 *
 * <p>Tous les paramètres numériques sont lus depuis {@link JobsConfig}.</p>
 */
public class CropGrowthListener implements Listener {

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
            int farmLevel = alpha.getJobLevel(EJob.FARMER);
            GameManager.getInstance().getPlantedCropsManager().addPlantedCrop(targetBlock.getLocation(), farmLevel);
        }
    }

    private static boolean isValidPlantingAttempt(PlayerInteractEvent event, AlphaPlayer alpha) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getItem() == null) return false;
        return PlantedCropUtils.isPlantable(event.getItem().getType());
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

        JobsConfig.FarmerCfg farmer = JobsConfig.getInstance().getFarmer();

        // Niv. 0-4 : ralentissement → annule le tick avec probabilité (1 - chance)
        if (farmLevel < 5 && Math.random() > farmer.getCropGrowthAllowChance()[farmLevel]) {
            event.setCancelled(true);
            return;
        }

        // Niv. 6-10 : tick(s) bonus après le tick normal
        if (farmLevel >= 6) {
            scheduleExtraGrowth(event.getBlock(), farmLevel, farmer);
        }
    }

    /**
     * Planifie un (ou deux pour niv.10) tick(s) de croissance supplémentaires.
     * N'avance le bloc que s'il est toujours un {@link Ageable} non au max.
     */
    private static void scheduleExtraGrowth(Block block, int farmLevel, JobsConfig.FarmerCfg farmer) {
        if (Math.random() >= farmer.getCropExtraGrowthChance()[farmLevel]) return;

        Location loc = block.getLocation().clone();
        MiubyLib.runLater(() -> {
            Block target = loc.getBlock();
            if (!(target.getBlockData() instanceof Ageable ageable)) return;
            if (ageable.getAge() >= ageable.getMaximumAge()) return;
            ageable.setAge(ageable.getAge() + 1);
            target.setBlockData(ageable);

            // Niv. 10 seulement : chance d'un 3e tick de croissance
            if (farmLevel == 10 && Math.random() < farmer.getCropThirdTickChanceAtMax()) {
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
    //  Bonemeal sur les cultures
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * La farine d'os est bloquée par défaut sur toutes les cultures trackées.
     * Pour les cultures plantées par un fermier, elle réussit selon {@code bone-meal-chance[niveau]}.
     * En cas d'échec, l'item est rendu au joueur.
     * Cultures non trackées (hors fermier) ou source non-joueur → toujours bloqué.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent event) {
        if (!PlantedCropUtils.isCrop(event.getBlock().getType())) return;

        Player player = event.getPlayer();
        PlantedCropsManager mgr = GameManager.getInstance().getPlantedCropsManager();
        Integer farmLevel = mgr.getFarmLevel(event.getBlock().getLocation());

        // Culture non plantée par un fermier ou source non-joueur → bone meal bloqué sans remboursement
        if (farmLevel == null || player == null) {
            event.setCancelled(true);
            return;
        }

        // Culture fermier : chance selon le niveau
        JobsConfig.FarmerCfg farmer = JobsConfig.getInstance().getFarmer();
        double chance = farmer.getBoneMealChance()[farmLevel];

        if (Math.random() >= chance) {
            event.setCancelled(true);
            player.getInventory().addItem(new ItemStack(Material.BONE_MEAL));
            MLLogManager.getInstance().log(Level.FINE, ELogTag.JOB,
                    "[CropGrowthListener.onFertilize] Échec farine d'os pour " + player.getName()
                            + " (niveau=" + farmLevel + ", chance=" + chance + ") — item rendu");
        }
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