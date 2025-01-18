package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.role.RoleAttribute;
import fr.miuby.survi.player.event.AlphaPlayerRoleChangeEvent;
import fr.miuby.survi.world.EWorld;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

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
}
