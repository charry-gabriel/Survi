package fr.miuby.survi.listener;

import fr.miuby.lib.world.MLWorld;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.log.LogManager;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.WorldInitializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

public class WorldListener implements Listener {

    // -------------------------------------------------------------------------
    // Portails
    // -------------------------------------------------------------------------

    /**
     * Bloque la création de portails Nether au Village.
     * Wilderness/Nether : comportement vanilla. End : géré via onEndPortalEnter.
     */
    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        if (event.getReason() != PortalCreateEvent.CreateReason.FIRE) return;
        if (event.getWorld().getName().equals(WorldInitializer.getWorlds().get(EWorld.VILLAGE))) {
            event.setCancelled(true);
        }
    }

    /**
     * Gère les portails Nether (Wilderness ↔ Nether).
     * Le portail End depuis Wilderness est intercepté par onEndPortalEnter (PlayerMoveEvent)
     * car PlayerPortalEvent ne fire pas pour les portails End vanilla dans un monde custom.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {

        if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            event.setCancelled(true);
            return;
        }

        if (WorldRegistry.get(EWorld.NETHER).isLocked()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c✖ Ce monde n'est pas encore accessible !");
            return;
        }

        World destination = getPortalDestination(event.getPlayer().getWorld().getName());

        if (destination == null) {
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.WORLD, "onPlayerPortal : destination inconnue pour " + event.getPlayer().getName());
            event.setCancelled(true);
            return;
        }

        Location to = event.getTo();
        event.setTo(new Location(destination, to.getX(), to.getY(), to.getZ()));
    }

    private @Nullable World getPortalDestination(String currentWorld) {
        String wildernessName = WorldInitializer.getWorlds().get(EWorld.WILDERNESS);
        String netherName = WorldInitializer.getWorlds().get(EWorld.NETHER);

        if (currentWorld.equals(wildernessName))
            return Bukkit.getWorld(netherName);

        if (currentWorld.equals(netherName))
            return Bukkit.getWorld(wildernessName);

        return null;
    }

    /**
     * Wilderness → End : PlayerPortalEvent ne fire pas pour les portails End vanilla
     * dans un monde custom, on détecte donc le bloc END_PORTAL via PlayerMoveEvent.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEndPortalEnter(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;

        Player player = event.getPlayer();
        String wild   = WorldInitializer.getWorlds().get(EWorld.WILDERNESS);
        if (!player.getWorld().getName().equals(wild)) return;
        if (player.getLocation().getBlock().getType() != Material.END_PORTAL) return;

        String endName = WorldInitializer.getWorlds().get(EWorld.END);
        MLWorld endMLWorld = WorldRegistry.get(EWorld.END);
        if (endMLWorld != null && endMLWorld.isLocked()) {
            player.sendMessage("§c✖ Ce monde n'est pas encore accessible !");
            return;
        }

        World endWorld = Bukkit.getWorld(endName);
        if (endWorld == null) {
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.WORLD, "onEndPortalEnter : monde End introuvable → " + endName);
            return;
        }

        player.teleport(endWorld.getSpawnLocation());
    }

    // -------------------------------------------------------------------------
    // Faux blocs portail & état joueur
    // -------------------------------------------------------------------------

    /**
     * Faux blocs portail + mise à jour du monde dans AlphaPlayer au changement de monde.
     */
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        GameManager.getInstance().getWorldPortalManager().sendFakePortalBlocksIfNeeded(event.getPlayer());

        AlphaPlayer alphaPlayer = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (alphaPlayer == null) return;

        MLWorld mlWorld = WorldRegistry.get(event.getPlayer().getWorld().getUID());
        if (mlWorld == null) {
            LogManager.getInstance().log(Level.WARNING, LogManager.ETagLog.WORLD,
                    "onPlayerChangedWorld : monde non enregistré pour " + event.getPlayer().getName()
                            + " → " + event.getPlayer().getWorld().getName()
                            + " (uid=" + event.getPlayer().getWorld().getUID() + "). AlphaPlayer.world non mis à jour.");
            return;
        }

        alphaPlayer.setWorld(mlWorld);
        GameManager.getInstance().getAlphaPlayerFactory().sendToPlayers(alphaPlayer);
        GameManager.getInstance().getAlphaPlayerFactory().getAttributeService().reapplyAllRoleAttributes(alphaPlayer);
    }

    /**
     * Téléportation via les faux portails Village ↔ Wilderness (blocs de verre).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        Player   player    = event.getPlayer();
        Location to        = event.getTo();
        GameManager.getInstance().getWorldPortalManager().teleportToWorld(player, to, to.getWorld().getName());
    }
}