package fr.miuby.survi.listener;

import fr.miuby.lib.world.MLWorld;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.WorldInitializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
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
    @EventHandler(ignoreCancelled = true)
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
            event.getPlayer().sendMessage(GameManager.getInstance().getLangService().text(event.getPlayer(), "world.locked"));
            return;
        }

        World destination = getPortalDestination(event.getPlayer().getWorld().getName());

        if (destination == null) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.WORLD, "onPlayerPortal : destination inconnue pour " + event.getPlayer().getName());
            event.setCancelled(true);
            return;
        }

        Location to = event.getTo();
        event.setTo(new Location(destination, to.getX(), to.getY(), to.getZ()));
    }

    private @Nullable World getPortalDestination(String currentWorld) {
        MLWorld wildernessMLWorld = WorldRegistry.get(EWorld.WILDERNESS);
        MLWorld netherMLWorld    = WorldRegistry.get(EWorld.NETHER);
        if (wildernessMLWorld == null || netherMLWorld == null) return null;

        if (currentWorld.equals(wildernessMLWorld.getWorld().getName()))
            return netherMLWorld.getWorld();

        if (currentWorld.equals(netherMLWorld.getWorld().getName()))
            return wildernessMLWorld.getWorld();

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

        MLWorld endMLWorld = WorldRegistry.get(EWorld.END);
        if (endMLWorld != null && endMLWorld.isLocked()) {
            player.sendMessage(GameManager.getInstance().getLangService().text(player, "world.locked"));
            return;
        }

        World endWorld = endMLWorld != null ? endMLWorld.getWorld() : null;
        if (endWorld == null) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.WORLD, "onEndPortalEnter : monde End introuvable dans le registry");
            return;
        }

        player.teleport(endWorld.getSpawnLocation());
    }

    // -------------------------------------------------------------------------
    // Faux blocs portail — envoi précis à la réception de chaque chunk
    // -------------------------------------------------------------------------

    /**
     * Envoie les faux blocs NETHER_PORTAL au client exactement 1 tick après que
     * Paper a transmis le chunk au joueur. Le délai garantit que le paquet BlockChange
     * arrive après le paquet LevelChunk dans la file TCP — peu importe la connexion
     * ou le render distance. Plus fiable que l'ancienne heuristique de 20 ticks.
     *
     * <p>Le filtre {@code chunkOverlapsPortal} court-circuite l'event pour les
     * ~99 % de chunks qui ne contiennent aucun bloc de portail.</p>
     */
    @EventHandler
    public void onPlayerChunkLoad(PlayerChunkLoadEvent event) {
        int cx = event.getChunk().getX();
        int cz = event.getChunk().getZ();
        World world = event.getChunk().getWorld();
        Player player = event.getPlayer();

        if (!GameManager.getInstance().getWorldPortalManager().chunkOverlapsPortal(world.getName(), cx, cz)) return;

        GameManager.getInstance().getPlugin().getServer().getScheduler().runTaskLater(
                GameManager.getInstance().getPlugin(), () -> {
                    if (!player.isOnline()) return;
                    if (!player.getWorld().equals(world)) return;
                    GameManager.getInstance().getWorldPortalManager().sendFakePortalBlocksInChunk(player, cx, cz);
                }, 1L);
    }

    // -------------------------------------------------------------------------
    // État joueur au changement de monde
    // -------------------------------------------------------------------------

    /**
     * Mise à jour du monde dans AlphaPlayer au changement de monde.
     * Les faux blocs portail sont gérés par {@link #onPlayerChunkLoad} — pas besoin
     * d'un appel explicite ici.
     */
    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        AlphaPlayer alphaPlayer = AlphaPlayer.get(event.getPlayer().getUniqueId());
        if (alphaPlayer == null) return;

        MLWorld mlWorld = WorldRegistry.get(event.getPlayer().getWorld().getUID());
        if (mlWorld == null) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.WORLD,
                    "onPlayerChangedWorld : monde non enregistré pour " + event.getPlayer().getName()
                            + " → " + event.getPlayer().getWorld().getName()
                            + " (uid=" + event.getPlayer().getWorld().getUID() + "). AlphaPlayer.world non mis à jour.");
            return;
        }

        alphaPlayer.setWorld(mlWorld);
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