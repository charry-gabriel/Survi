package fr.miuby.survi.listener;

import fr.miuby.survi.item.growth_item.GrowthItems;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class GrowthItemListener  implements Listener {
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        GrowthItems.IncrementUses(event.getPlayer(), "BlockBreakEvent");
    }
}
