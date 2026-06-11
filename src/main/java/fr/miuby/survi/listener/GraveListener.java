package fr.miuby.survi.listener;

import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.system.lang.LangKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class GraveListener implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (WorldRegistry.get(event.getPlayer().getWorld().getUID()).getType() == EWorld.VILLAGE)
            return;

        boolean created = GameManager.getInstance().getGraveManager().createGrave(event.getEntity());
        if (created) {
            event.getDrops().clear();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null)
            return;

        if (GameManager.getInstance().getGraveManager().collectGrave(event.getPlayer(), event.getClickedBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (GameManager.getInstance().getGraveManager().isGrave(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(GameManager.getInstance().getLangService().text(event.getPlayer(), LangKey.GRAVE_INDESTRUCTIBLE));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> GameManager.getInstance().getGraveManager().isGrave(block.getLocation()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> GameManager.getInstance().getGraveManager().isGrave(block.getLocation()));
    }
}