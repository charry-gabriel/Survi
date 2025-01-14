package fr.miuby.survi.listener;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.role.Role;
import fr.miuby.survi.role.RoleAttribute;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.Monde;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.world.PortalCreateEvent;

public class WorldListener implements Listener {
    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        if (event.getReason().equals(PortalCreateEvent.CreateReason.FIRE)) {
            if (Monde.get(EWorld.NETHER).isLocked())
                event.setCancelled(true);
        } else if (event.getReason().equals(PortalCreateEvent.CreateReason.END_PLATFORM)) {
            if (Monde.get(EWorld.END).isLocked())
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        AlphaPlayer alphaPlayer = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (alphaPlayer == null)
            return;

        for (RoleAttribute attribute : alphaPlayer.getRole().attributes()) {
            AttributeInstance playerAttribute = event.getPlayer().getAttribute(attribute.getAttributeType());
            if (playerAttribute != null) {
                AttributeModifier attributeModifier = playerAttribute.getModifier(new NamespacedKey(GameManager.getInstance().getPlugin(), RoleAttribute.createName(EWorld.get(event.getFrom()).toString(), attribute.getRole(), attribute.getAttributeType().getKey().getKey())));
                if (attributeModifier != null) {
                    playerAttribute.removeModifier(attributeModifier);
                }
            }
        }

        for (Role role : alphaPlayer.getSubRoles()) {
            for (RoleAttribute attribute : role.attributes()) {
                AttributeInstance playerAttribute = event.getPlayer().getAttribute(attribute.getAttributeType());
                if (playerAttribute != null) {
                    AttributeModifier attributeModifier = playerAttribute.getModifier(new NamespacedKey(GameManager.getInstance().getPlugin(), RoleAttribute.createName(EWorld.get(event.getFrom()).toString(), attribute.getRole(), attribute.getAttributeType().getKey().getKey())));
                    if (attributeModifier != null) {
                        playerAttribute.removeModifier(attributeModifier);
                    }
                }
            }
        }

        alphaPlayer.switchWorld();
    }
}
