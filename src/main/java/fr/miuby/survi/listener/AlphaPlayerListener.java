package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.role.ERole;
import fr.miuby.survi.role.Role;
import fr.miuby.survi.role.RoleAttribute;
import fr.miuby.survi.player.event.AlphaPlayerRoleChangeEvent;
import fr.miuby.survi.world.EWorld;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;

public class AlphaPlayerListener implements Listener {

    @EventHandler
    public void onAlphaPlayerRoleChange(AlphaPlayerRoleChangeEvent event) {
        if (!event.getAlphaPlayer().getPlayer().isOnline())
            return;

        // remove old attribute
        if (event.getOldRole() != null) {
            for (RoleAttribute attribute : event.getOldRole().attributes()) {
                AttributeInstance playerAttribute = event.getAlphaPlayer().getPlayer().getAttribute(attribute.getAttributeType());
                if (playerAttribute != null) {
                    AttributeModifier attributeModifier = playerAttribute.getModifier(new NamespacedKey(GameManager.getInstance().getPlugin(), RoleAttribute.createName(attribute.getWorld().toString(), attribute.getRole(), attribute.getAttributeType().getKey().getKey())));
                    if (attributeModifier != null) {
                        if (attribute.getAttributeType() == Attribute.MAX_HEALTH)
                            event.getAlphaPlayer().getAlphaLife().regenHealth(()-> playerAttribute.removeModifier(attributeModifier));
                        else
                            playerAttribute.removeModifier(attributeModifier);
                    }
                }
            }
        }

        // add new attribute
        if (event.getNewRole() != null) {
            for (RoleAttribute attribute : event.getNewRole().attributes()) {
                if ((event.getAlphaPlayer().getWorld() == attribute.getWorld() || attribute.getWorld() == EWorld.ALL)) {
                    attribute.setRole(event.getNewRole().roleId());
                    if (attribute.getAttributeType() == Attribute.MAX_HEALTH)
                        event.getAlphaPlayer().getAlphaLife().regenHealth(() -> event.getAlphaPlayer().addAttribute(attribute));
                    else
                        event.getAlphaPlayer().addAttribute(attribute);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        
        if (pdc.has(new NamespacedKey(GameManager.getInstance().getPlugin(), "edible"))) {
            AlphaPlayer alphaPlayer = AlphaPlayer.get(event.getPlayer().getUniqueId());
            
            Role newRole = GameManager.getInstance().getRoleFactory().getRole(ERole.MINEUR);
            AlphaPlayerRoleChangeEvent roleChangeEvent = new AlphaPlayerRoleChangeEvent(alphaPlayer, null, newRole);
            GameManager.getInstance().callEvent(roleChangeEvent);
            
            if (!roleChangeEvent.isCancelled()) {
                alphaPlayer.addSubRole(newRole);
                
                // Mettre à jour l'affichage pour les autres joueurs
                if (alphaPlayer.getPlayer() != null && alphaPlayer.getPlayer().isOnline())
                    GameManager.getInstance().getAlphaPlayerFactory().sendToPlayers(alphaPlayer);
            } else {
                event.setCancelled(true);
            }
        }
    }
}
