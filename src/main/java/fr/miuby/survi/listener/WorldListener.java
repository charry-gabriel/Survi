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

import java.util.logging.Level;

public class WorldListener implements Listener {

    /**
     * Bloque uniquement la création de portails nether dans Village
     * (empêche les joueurs d'allumer un portail en obsidienne au Village).
     *
     * Wilderness et son Nether : portails vanilla autorisés (comportement normal).
     * End : entièrement vanilla, aucune interférence.
     */
    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        if (event.getReason() != PortalCreateEvent.CreateReason.FIRE) return;

        World world = event.getWorld();
        String villageName = WorldInitializer.getWorlds().get(EWorld.VILLAGE);
        if (world.getName().equals(villageName)) {
            event.setCancelled(true);
        }
    }

    /**
     * Mise à jour du monde du joueur dans AlphaPlayer lors d'un changement de monde.
     *
     * Note : WorldPortalManager écoute aussi PlayerChangedWorldEvent pour envoyer
     * les faux blocs NETHER_PORTAL — les deux listeners coexistent sans conflit.
     */
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        GameManager.getInstance().getWorldPortalManager().sendFakePortalBlocksIfNeeded(event.getPlayer());

        AlphaPlayer alphaPlayer = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (alphaPlayer == null) return;

        MLWorld mlWorld = WorldRegistry.get(event.getPlayer().getWorld().getUID());
        if (mlWorld == null) {
            LogManager.getInstance().log(Level.WARNING, LogManager.ETagLog.WORLD,
                    "onPlayerChangedWorld : monde non enregistré pour "
                            + event.getPlayer().getName()
                            + " → " + event.getPlayer().getWorld().getName()
                            + " (uid=" + event.getPlayer().getWorld().getUID() + ")"  // ← uid utile pour debug
                            + ". AlphaPlayer.world non mis à jour.");
            return;
        }

        alphaPlayer.setWorld(mlWorld);
        GameManager.getInstance().getAlphaPlayerFactory().sendToPlayers(alphaPlayer);
        GameManager.getInstance().getAlphaPlayerFactory().getAttributeService().reapplyAllRoleAttributes(alphaPlayer);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;

        Player   player    = event.getPlayer();
        Location to        = event.getTo();
        String   worldName = to.getWorld().getName();

        GameManager.getInstance().getWorldPortalManager().teleportToWorld(player, to, worldName);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) return;

        Player player = event.getPlayer();
        String fromWorld = player.getWorld().getName();

        String wildName   = WorldInitializer.getWorlds().get(EWorld.WILDERNESS);
        String netherName = WorldInitializer.getWorlds().get(EWorld.NETHER);

        // Wilderness → Nether
        if (fromWorld.equals(wildName)) {
            World nether = Bukkit.getWorld(netherName);
            if (nether == null) { event.setCancelled(true); return; }

            Location from = player.getLocation();
            event.setTo(new Location(nether, from.getX() / 8.0, from.getY(), from.getZ() / 8.0));
            return;
        }

        // Nether → Wilderness
        if (fromWorld.equals(netherName)) {
            World wild = Bukkit.getWorld(wildName);
            if (wild == null) { event.setCancelled(true); return; }

            Location from = player.getLocation();
            event.setTo(new Location(wild, from.getX() * 8.0, from.getY(), from.getZ() * 8.0));
            return;
        }

        // Tout autre monde (Village, End...) → bloquer le portail nether vanilla
        event.setCancelled(true);
    }
}