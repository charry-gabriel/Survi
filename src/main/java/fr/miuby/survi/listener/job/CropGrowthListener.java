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
 * Gère la vitesse de croissance des cultures (fermier) et des pousses d'arbres (bûcheron).
 *
 * <ul>
 *   <li>Cultures plantées par un fermier (niv. 0-4) → croissance ralentie.</li>
 *   <li>Cultures plantées par un fermier (niv. 5) → croissance vanilla.</li>
 *   <li>Cultures plantées par un fermier (niv. 6-10) → croissance accélérée (tick(s) bonus).</li>
 *   <li>Pousses plantées par un bûcheron (niv. 0-4) → croissance ralentie.</li>
 *   <li>Pousses plantées par un bûcheron (niv. 5) → croissance vanilla.</li>
 *   <li>Pousses plantées par un bûcheron (niv. 6-10) → croissance accélérée (tick(s) bonus).</li>
 *   <li>Tout le reste → croissance vanilla (aucune modification).</li>
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
            boolean isSapling = PlantedCropUtils.isSapling(event.getItem().getType());
            int level = isSapling ? alpha.getJobLevel(EJob.LUMBERJACK) : alpha.getJobLevel(EJob.FARMER);
            GameManager.getInstance().getPlantedCropsManager().addPlantedCrop(targetBlock.getLocation(), level);
        }
    }

    private static boolean isValidPlantingAttempt(PlayerInteractEvent event, AlphaPlayer alpha) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getItem() == null) return false;
        return PlantedCropUtils.isPlantable(event.getItem().getType());
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Croissance des cultures et des pousses
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropGrow(BlockGrowEvent event) {
        Material type = event.getBlock().getType();
        if (!PlantedCropUtils.isCrop(type)) return;

        PlantedCropsManager mgr = GameManager.getInstance().getPlantedCropsManager();
        Integer storedLevel = mgr.getFarmLevel(event.getBlock().getLocation());

        // Non planté par un joueur → croissance vanilla
        if (storedLevel == null) return;

        if (PlantedCropUtils.isSapling(type)) {
            handleSaplingGrow(event, storedLevel);
        } else {
            handleCropGrow(event, storedLevel);
        }
    }

    private static void handleSaplingGrow(BlockGrowEvent event, int lumberjackLevel) {
        JobsConfig.LumberjackCfg lj = JobsConfig.getInstance().getLumberjack();

        if (lumberjackLevel < 5 && Math.random() > lj.getSaplingGrowthAllowChance()[lumberjackLevel]) {
            event.setCancelled(true);
            MLLogManager.getInstance().log(Level.FINE, ELogTag.JOB,
                    "[CropGrowthListener.onCropGrow] Tick sapling annulé pour bûcheron niv." + lumberjackLevel
                            + " (chance=" + lj.getSaplingGrowthAllowChance()[lumberjackLevel] + ") @ " + event.getBlock().getLocation());
            return;
        }

        if (lumberjackLevel >= 6) {
            scheduleExtraGrowth(event.getBlock(), lumberjackLevel, lj.getSaplingExtraGrowthChance(), lj.getSaplingThirdTickChanceAtMax());
        }
    }

    private static void handleCropGrow(BlockGrowEvent event, int farmLevel) {
        JobsConfig.FarmerCfg farmer = JobsConfig.getInstance().getFarmer();

        if (farmLevel < 5 && Math.random() > farmer.getCropGrowthAllowChance()[farmLevel]) {
            event.setCancelled(true);
            return;
        }

        if (farmLevel >= 6) {
            scheduleExtraGrowth(event.getBlock(), farmLevel, farmer.getCropExtraGrowthChance(), farmer.getCropThirdTickChanceAtMax());
        }
    }

    /**
     * Planifie un (ou deux pour niv.10) tick(s) de croissance supplémentaires.
     * N'avance le bloc que s'il est toujours un {@link Ageable} non au max.
     */
    private static void scheduleExtraGrowth(Block block, int level, double[] extraGrowthChance, double thirdTickChanceAtMax) {
        if (Math.random() >= extraGrowthChance[level]) return;

        Location loc = block.getLocation().clone();
        MiubyLib.runLater(() -> {
            Block target = loc.getBlock();
            if (!(target.getBlockData() instanceof Ageable ageable)) return;
            if (ageable.getAge() >= ageable.getMaximumAge()) return;
            ageable.setAge(ageable.getAge() + 1);
            target.setBlockData(ageable);

            // Niv. 10 seulement : chance d'un 3e tick de croissance
            if (level == 10 && Math.random() < thirdTickChanceAtMax) {
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
    //  Bonemeal sur les cultures et les pousses
    // ════════════════════════════════════════════════════════════════════════════

    /**
     * La farine d'os est bloquée par défaut sur toutes les cultures/pousses trackées.
     * Pour les cultures fermier, elle réussit selon {@code bone-meal-chance[niveauFermier]}.
     * Pour les pousses bûcheron, elle réussit selon {@code sapling-bone-meal-chance[niveauBûcheron]}.
     * En cas d'échec ou si le bloc n'est pas tracké, l'event est annulé et la farine est consommée.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent event) {
        if (!PlantedCropUtils.isCrop(event.getBlock().getType())) return;

        Player player = event.getPlayer();
        PlantedCropsManager mgr = GameManager.getInstance().getPlantedCropsManager();
        Integer storedLevel = mgr.getFarmLevel(event.getBlock().getLocation());

        // Non tracké (pas planté par un joueur) ou source non-joueur → bloqué
        if (storedLevel == null || player == null) {
            event.setCancelled(true);
            if (player != null) consumeBoneMeal(player);
            return;
        }

        boolean isSapling = PlantedCropUtils.isSapling(event.getBlock().getType());
        AlphaPlayer alpha = AlphaPlayer.get(player.getUniqueId());

        double chance;
        int playerLevel;
        if (isSapling) {
            playerLevel = alpha != null ? alpha.getJobLevel(EJob.LUMBERJACK) : 0;
            chance = JobsConfig.getInstance().getLumberjack().getSaplingBoneMealChance()[playerLevel];
        } else {
            playerLevel = alpha != null ? alpha.getJobLevel(EJob.FARMER) : 0;
            chance = JobsConfig.getInstance().getFarmer().getBoneMealChance()[playerLevel];
        }

        if (Math.random() >= chance) {
            event.setCancelled(true);
            consumeBoneMeal(player);
            MLLogManager.getInstance().log(Level.FINE, ELogTag.JOB,
                    "[CropGrowthListener.onFertilize] Échec farine d'os pour " + player.getName()
                            + (isSapling ? " (bûcheron" : " (fermier")
                            + " niveau=" + playerLevel + ", chance=" + chance + ") — item consommé");
        }
    }

    /**
     * Retire une farine d'os de l'inventaire du joueur (main principale puis off-hand).
     * Appelé manuellement après {@code event.setCancelled(true)} sur un {@link BlockFertilizeEvent},
     * car Paper ne consomme pas l'item quand l'event est annulé.
     */
    private static void consumeBoneMeal(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main.getType() == Material.BONE_MEAL) {
            main.setAmount(main.getAmount() - 1);
            return;
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off.getType() == Material.BONE_MEAL) {
            off.setAmount(off.getAmount() - 1);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  Nettoyage du registre à la récolte / destruction
    // ════════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (PlantedCropUtils.isCrop(event.getBlock().getType()))
            GameManager.getInstance().getPlantedCropsManager().removePlantedCrop(event.getBlock().getLocation());
    }
}