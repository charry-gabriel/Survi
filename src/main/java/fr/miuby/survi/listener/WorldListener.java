package fr.miuby.survi.listener;

import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.Monde;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class WorldListener implements Listener {
    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        if (event.getReason().equals(PortalCreateEvent.CreateReason.FIRE)) {
            if (Monde.get(EWorld.NETHER).isLocked())
                event.setCancelled(true);
        } else {
            if (Monde.get(EWorld.END).isLocked())
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        AlphaPlayer.get(event.getPlayer().getUniqueId()).switchWorld();
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.getWorld() != Monde.get(EWorld.NETHER).getWorld())
            return;

        for (BlockState blockState : event.getChunk().getTileEntities()) {
            if (blockState instanceof Chest chest) {
                Inventory inventory = chest.getInventory();

                for (ItemStack stack : inventory.getContents()) {
                    if (stack != null
                            && (stack.getType() == Material.DIAMOND_BOOTS
                            || stack.getType() == Material.DIAMOND_CHESTPLATE
                            || stack.getType() == Material.DIAMOND_HELMET
                            || stack.getType() == Material.DIAMOND_LEGGINGS)) {
                        inventory.remove(stack);
                    }
                }
            }
        }
    }
}
