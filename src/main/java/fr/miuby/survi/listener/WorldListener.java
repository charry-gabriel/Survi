package fr.miuby.survi.listener;

import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.role.Role;
import fr.miuby.survi.role.RoleAttribute;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.WorldFactory;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class WorldListener implements Listener {
    private boolean worldsInitialized = false;

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        if (!worldsInitialized && areAllWorldsLoaded()) {
            GameManager.getInstance().initAfterWorldsLoad();
            worldsInitialized = true;
        }
    }

    private boolean areAllWorldsLoaded() {
        for (String worldName : WorldFactory.getWorlds().values()) {
            if (GameManager.getInstance().getPlugin().getServer().getWorld(worldName) == null) {
                return false;
            }
        }
        return true;
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        if (event.getReason().equals(PortalCreateEvent.CreateReason.FIRE)) {
            if (WorldRegistry.get(EWorld.NETHER).isLocked())
                event.setCancelled(true);
        } else if (event.getReason().equals(PortalCreateEvent.CreateReason.END_PLATFORM)) {
            if (WorldRegistry.get(EWorld.END).isLocked())
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        AlphaPlayer alphaPlayer = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (alphaPlayer == null)
            return;

        // change alphaPlayer world
        alphaPlayer.setWorld(WorldRegistry.get(event.getPlayer().getWorld().getUID()));
        GameManager.getInstance().getAlphaPlayerFactory().sendToPlayers(alphaPlayer);

        // remove old attribute
        for (RoleAttribute attribute : alphaPlayer.getRole().attributes()) {
            AttributeInstance playerAttribute = event.getPlayer().getAttribute(attribute.getAttributeType());
            if (playerAttribute != null) {
                AttributeModifier attributeModifier = playerAttribute.getModifier(new NamespacedKey(GameManager.getInstance().getPlugin(), RoleAttribute.createName(WorldRegistry.get(event.getFrom().getUID()).getType().toString(), attribute.getRole(), attribute.getAttributeType().getKey().getKey())));
                if (attributeModifier != null) {
                    if (attribute.getAttributeType() == Attribute.MAX_HEALTH)
                        alphaPlayer.getAlphaLife().regenHealth(() -> playerAttribute.removeModifier(attributeModifier));
                    else
                        playerAttribute.removeModifier(attributeModifier);
                }
            }
        }

        for (Role role : alphaPlayer.getSubRoles()) {
            for (RoleAttribute attribute : role.attributes()) {
                AttributeInstance playerAttribute = event.getPlayer().getAttribute(attribute.getAttributeType());
                if (playerAttribute != null) {
                    AttributeModifier attributeModifier = playerAttribute.getModifier(new NamespacedKey(GameManager.getInstance().getPlugin(), RoleAttribute.createName(WorldRegistry.get(event.getFrom().getUID()).getType().toString(), attribute.getRole(), attribute.getAttributeType().getKey().getKey())));
                    if (attributeModifier != null) {
                        if (attribute.getAttributeType() == Attribute.MAX_HEALTH)
                            alphaPlayer.getAlphaLife().regenHealth(() -> playerAttribute.removeModifier(attributeModifier));
                        else
                            playerAttribute.removeModifier(attributeModifier);
                    }
                }
            }
        }

        // add new attribute
        alphaPlayer.addRoleAttribute();
    }
}
