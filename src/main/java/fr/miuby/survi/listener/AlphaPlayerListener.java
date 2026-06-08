package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.player.event.AlphaPlayerRoleChangeEvent;
import fr.miuby.survi.role.ERole;
import fr.miuby.survi.role.Role;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;

public class AlphaPlayerListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onAlphaPlayerRoleChange(AlphaPlayerRoleChangeEvent event) {
        if (!event.getAlphaPlayer().getPlayer().isOnline())
            return;
        GameManager.getInstance().getAlphaPlayerFactory().getAttributeService().onRoleAttributesChange(
                event.getAlphaPlayer(), event.getOldRole(), event.getNewRole());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        
        if (pdc.has(new NamespacedKey(GameManager.getInstance().getPlugin(), "edible"))) {
            AlphaPlayer alphaPlayer = AlphaPlayer.get(event.getPlayer().getUniqueId());
            
            Role newRole = GameManager.getInstance().getRoleLoader().getRole(ERole.MINEUR);
            AlphaPlayerRoleChangeEvent roleChangeEvent = new AlphaPlayerRoleChangeEvent(alphaPlayer, null, newRole);
            GameManager.getInstance().callEvent(roleChangeEvent);
            
            if (!roleChangeEvent.isCancelled()) {
                alphaPlayer.addSubRole(newRole);
            } else {
                event.setCancelled(true);
            }
        }
    }
}
