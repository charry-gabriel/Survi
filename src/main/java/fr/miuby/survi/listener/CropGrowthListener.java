package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.world.crops.PlantedCropUtils;
import fr.miuby.survi.world.crops.PlantedCropsManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.role.ERole;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class CropGrowthListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPlant(PlayerInteractEvent event) {
        if (!isValidPlantingAttempt(event))
            return;
        
        Block targetBlock = GameManager.getInstance().getPlantedCropsManager().getTargetBlockForPlanting(event);
        
        if (targetBlock != null)
            GameManager.getInstance().getPlantedCropsManager().addPlantedCrop(targetBlock.getLocation());
    }

    private boolean isValidPlantingAttempt(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getItem() == null)
            return false;

        Material itemType = event.getItem().getType();
        if (!PlantedCropUtils.isPlantable(itemType))
            return false;

        return AlphaPlayer.get(event.getPlayer().getUniqueId()).getSubRoles().stream().anyMatch(role -> role.type() == ERole.FERMIER);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCropGrow(BlockGrowEvent event) {
        if (!PlantedCropUtils.isCrop(event.getBlock().getType()))
            return;

        if (GameManager.getInstance().getPlantedCropsManager().isPlantedByFarmer(event.getBlock().getLocation()))
            return;

        if (Math.random() > PlantedCropsManager.WILD_GROWTH_CHANCE)
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent event) {
        if (PlantedCropUtils.isCrop(event.getBlock().getType()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (PlantedCropUtils.isCrop(event.getBlock().getType()))
            GameManager.getInstance().getPlantedCropsManager().removePlantedCrop(event.getBlock().getLocation());
    }
}
