package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.role.RoleAttribute;
import fr.miuby.survi.player.event.AlphaPlayerRoleChangeEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AlphaPlayerListener implements Listener {

    @EventHandler
    public void onAlphaPlayerRoleChange(AlphaPlayerRoleChangeEvent event) {
        if (event.getOldRole() == null)
            return;

        for (RoleAttribute attribute : event.getOldRole().attributes()) {
            AttributeInstance playerAttribute = event.getAlphaPlayer().getPlayer().getAttribute(attribute.getAttributeType());
            if (playerAttribute != null) {
                AttributeModifier attributeModifier = playerAttribute.getModifier(new NamespacedKey(GameManager.getInstance().getPlugin(), RoleAttribute.createName(attribute.getWorld().toString(), attribute.getRole(), attribute.getAttributeType().getKey().getKey())));
                if (attributeModifier != null) {
                    playerAttribute.removeModifier(attributeModifier);
                }
            }
        }



    }
}
