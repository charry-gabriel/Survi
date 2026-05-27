package fr.miuby.survi.world;

import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.log.LogManager;
import org.bukkit.*;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class WorldPortalManager {

    /** Délai (ticks) après le chargement serveur des chunks pour laisser le client les recevoir. */
    private static final long CLIENT_CHUNK_RECEIVE_DELAY = 20L;

    private static final String DB_KEY_WORLD  = "world_portal_wild_world";
    private static final String DB_KEY_MIN_X  = "world_portal_wild_min_x";
    private static final String DB_KEY_MIN_Y  = "world_portal_wild_min_y";
    private static final String DB_KEY_MIN_Z  = "world_portal_wild_min_z";
    private static final String DB_KEY_MAX_X  = "world_portal_wild_max_x";
    private static final String DB_KEY_MAX_Y  = "world_portal_wild_max_y";
    private static final String DB_KEY_MAX_Z  = "world_portal_wild_max_z";

    // worldName → { min, max } de l'intérieur du portail (blocs de verre)
    private final Map<String, Location[]> portalZones = new HashMap<>();

    // Anti-spam téléportation (cooldown 1 s)
    private final Set<UUID> teleporting = new HashSet<>();

    public WorldPortalManager() {}

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    public void init() {
        loadWildernessPortalFromDB();

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.WORLD, "WorldPortalManager initialisé.");
    }

    // -------------------------------------------------------------------------
    // Enregistrement du portail Wilderness (appelé par WorldResetManager)
    // -------------------------------------------------------------------------

    /**
     * Enregistre la zone intérieure (blocs de verre) du portail Wilderness.
     * Sauvegarde min et max en DB, puis envoie les faux blocs aux joueurs présents
     * dans ce monde une fois les chunks chargés côté serveur et client.
     */
    public void registerWildernessPortal(String worldName, Location min, Location max) {
        portalZones.put(worldName, new Location[]{ min.clone(), max.clone() });
        saveWildernessPortalToDB(worldName, min, max);

        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            world.getPlayers().forEach(p -> sendFakePortalBlocksAsync(p, min, max));
        }

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.WORLD,
                "Portail Wilderness enregistré : " + worldName + " " + fmt(min) + " → " + fmt(max) + " (axe : " + detectAxis(min, max) + ")");
    }

    /**
     * Met à jour la position du portail Village (appelé par {@link VillageZoneManager}
     * à chaque changement de palier de zone).
     *
     * <p>Enregistre la nouvelle zone dans {@link #portalZones} et envoie immédiatement
     * les faux blocs NETHER_PORTAL à tous les joueurs présents dans le Village.</p>
     */
    public void updateVillagePortal(String villageName, Location min, Location max) {
        World world = Bukkit.getWorld(villageName);

        // ── Effacer les anciens faux blocs si une zone existait déjà ──────────────
        Location[] oldZone = portalZones.get(villageName);
        if (oldZone != null && world != null) {
            world.getPlayers().forEach(p -> clearFakePortalBlocks(p, oldZone[0], oldZone[1]));
        }

        // ── Enregistrer la nouvelle zone et envoyer les nouveaux faux blocs ───────
        portalZones.put(villageName, new Location[]{ min.clone(), max.clone() });

        if (world != null) {
            world.getPlayers().forEach(p -> sendFakePortalBlocksAsync(p, min, max));
        }

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.WORLD,
                "Portail Village mis à jour : " + fmt(min) + " → " + fmt(max)
                        + " (axe : " + detectAxis(min, max) + ")");
    }

    /** Retire la zone du portail Wilderness avant unload (reset). */
    public void unregisterWildernessPortal(String worldName) {
        if (portalZones.remove(worldName) != null) {
            LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.WORLD, "Portail Wilderness désenregistré : " + worldName);
        }
    }

    // -------------------------------------------------------------------------
    // Point d'entrée unique : connexion ou changement de monde
    // -------------------------------------------------------------------------

    /**
     * À appeler depuis {@code PlayerJoinEvent} ET {@code PlayerChangedWorldEvent}.
     * Charge les chunks du portail côté serveur si nécessaire, attend que le client
     * les ait reçus, puis envoie les faux blocs NETHER_PORTAL.
     */
    public void sendFakePortalBlocksIfNeeded(Player player) {
        Location[] zone = portalZones.get(player.getWorld().getName());
        if (zone == null) return;

        sendFakePortalBlocksAsync(player, zone[0], zone[1]);
    }

    public void sendAllFakePortalBlocks(Player player) {
        portalZones.values().forEach(zone -> sendFakePortalBlocksAsync(player, zone[0], zone[1]));
    }

    // -------------------------------------------------------------------------
    // Envoi async : chargement chunk serveur → délai client → sendBlockChange
    // -------------------------------------------------------------------------

    /**
     * Charge en async tous les chunks couvrant la zone [min, max] du portail,
     * puis attend {@link #CLIENT_CHUNK_RECEIVE_DELAY} ticks pour laisser le client
     * recevoir les chunks, puis envoie les faux blocs NETHER_PORTAL au joueur.
     *
     * <p>Deux étapes garantissent que {@code sendBlockChange} arrive après que
     * le client a le chunk en mémoire :</p>
     * <ol>
     *   <li>Chargement async serveur (world.getChunkAtAsync)</li>
     *   <li>Délai réseau client (CLIENT_CHUNK_RECEIVE_DELAY ticks)</li>
     * </ol>
     */
    private void sendFakePortalBlocksAsync(Player player, Location min, Location max) {
        World world = min.getWorld();
        if (world == null) return;

        int minCX = Math.min(min.getBlockX(), max.getBlockX()) >> 4;
        int maxCX = Math.max(min.getBlockX(), max.getBlockX()) >> 4;
        int minCZ = Math.min(min.getBlockZ(), max.getBlockZ()) >> 4;
        int maxCZ = Math.max(min.getBlockZ(), max.getBlockZ()) >> 4;

        List<CompletableFuture<Chunk>> futures = new ArrayList<>();
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                futures.add(world.getChunkAtAsync(cx, cz));
            }
        }

        // Étape 1 : attendre que tous les chunks soient chargés côté serveur
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() ->
                // Étape 2 : petit délai pour que le client reçoive les chunks via réseau
                Bukkit.getScheduler().runTaskLater(GameManager.getInstance().getPlugin(), () -> {
                    if (!player.isOnline()) return;
                    if (!player.getWorld().equals(world)) return; // changement de monde entre-temps

                    LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.WORLD,
                            "Envoi faux blocs portail à " + player.getName() + " dans " + world.getName());
                    sendFakePortalBlocks(player, min, max);
                }, CLIENT_CHUNK_RECEIVE_DELAY)
        );
    }

    // -------------------------------------------------------------------------
    // Envoi bas niveau
    // -------------------------------------------------------------------------

    public void sendFakePortalBlocks(Player player, Location min, Location max) {
        Axis axis = detectAxis(min, max);

        var fakePortal = Bukkit.createBlockData(Material.NETHER_PORTAL,
                bd -> ((Orientable) bd).setAxis(axis));

        iterateZone(min, max, (world, x, y, z) -> {
            player.sendBlockChange(new Location(world, x, y, z), fakePortal);
            LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.WORLD,
                    world.getName() + " : " + x + "," + y + "," + z + " -> FAKE_NETHER_PORTAL");
        });
    }

    /**
     * Envoie des faux blocs AIR à l'ancienne position du portail pour effacer
     * les NETHER_PORTAL côté client. Utilise le vrai type du bloc serveur pour
     * ne pas écraser un bloc réel si la structure a changé entre-temps.
     */
    public void clearFakePortalBlocks(Player player, Location min, Location max) {
        iterateZone(min, max, (world, x, y, z) -> {
            Location loc = new Location(world, x, y, z);
            // On renvoie le vrai bloc serveur — annule le fake sans toucher au décor réel
            player.sendBlockChange(loc, world.getBlockAt(loc).getBlockData());
            LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.WORLD,
                    world.getName() + " : " + x + "," + y + "," + z + " -> CLEAR_FAKE_PORTAL");
        });
    }

    /** Itère sur tous les blocs de la zone [min, max] et applique {@code action}. */
    @FunctionalInterface
    private interface BlockAction {
        void apply(World world, int x, int y, int z);
    }

    private void iterateZone(Location min, Location max, BlockAction action) {
        World world = min.getWorld();
        if (world == null) return;

        int minX = Math.min(min.getBlockX(), max.getBlockX());
        int maxX = Math.max(min.getBlockX(), max.getBlockX());
        int minY = Math.min(min.getBlockY(), max.getBlockY());
        int maxY = Math.max(min.getBlockY(), max.getBlockY());
        int minZ = Math.min(min.getBlockZ(), max.getBlockZ());
        int maxZ = Math.max(min.getBlockZ(), max.getBlockZ());

        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++)
                    action.apply(world, x, y, z);
    }

    // -------------------------------------------------------------------------
    // Téléportation — PlayerMoveEvent
    // -------------------------------------------------------------------------

    public void teleportTowWorld(Player player, Location to, String worldName) {
        Location[] zone = portalZones.get(worldName);
        if (zone == null) return;
        if (!isInPortalZone(to, zone[0], zone[1])) return;
        if (teleporting.contains(player.getUniqueId())) return;

        String villageName = WorldInitializer.getWorlds().get(EWorld.VILLAGE);
        String wildName    = WorldInitializer.getWorlds().get(EWorld.WILDERNESS);

        if (worldName.equals(villageName)) {
            triggerTeleport(player, EWorld.WILDERNESS, "§6[Portail] §eVous entrez dans la Wilderness !");
        } else if (worldName.equals(wildName)) {
            triggerTeleport(player, EWorld.VILLAGE, "§6[Portail] §eRetour au Village !");
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Axis detectAxis(Location min, Location max) {
        if (min.getBlockZ() == max.getBlockZ()) return Axis.X;
        if (min.getBlockX() == max.getBlockX()) return Axis.Z;
        return Axis.X;
    }

    private boolean isInPortalZone(Location loc, Location min, Location max) {
        int minX = Math.min(min.getBlockX(), max.getBlockX());
        int maxX = Math.max(min.getBlockX(), max.getBlockX());
        int minY = Math.min(min.getBlockY(), max.getBlockY());
        int maxY = Math.max(min.getBlockY(), max.getBlockY());
        int minZ = Math.min(min.getBlockZ(), max.getBlockZ());
        int maxZ = Math.max(min.getBlockZ(), max.getBlockZ());

        Axis axis = detectAxis(min, max);
        boolean inY = loc.getY() >= minY && loc.getY() < maxY + 1.0;

        if (axis == Axis.X) {
            boolean inX = loc.getX() >= minX - 0.9 && loc.getX() <= maxX + 1.9;
            boolean inZ = loc.getZ() >= minZ - 1.2 && loc.getZ() <= maxZ + 1.2;
            return inX && inY && inZ;
        } else {
            boolean inX = loc.getX() >= minX - 1.2 && loc.getX() <= maxX + 1.2;
            boolean inZ = loc.getZ() >= minZ - 0.9 && loc.getZ() <= maxZ + 1.9;
            return inX && inY && inZ;
        }
    }

    private void triggerTeleport(Player player, EWorld destination, String message) {
        teleporting.add(player.getUniqueId());

        var mlWorld = WorldRegistry.get(destination);
        if (mlWorld == null) {
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.WORLD, "triggerTeleport : monde non enregistré → " + destination);
            teleporting.remove(player.getUniqueId());
            return;
        }

        player.teleport(mlWorld.getWorld().getSpawnLocation());
        player.sendMessage(message);

        Bukkit.getScheduler().runTaskLater(GameManager.getInstance().getPlugin(), () -> teleporting.remove(player.getUniqueId()), 20L);
    }

    // -------------------------------------------------------------------------
    // Persistance DB
    // -------------------------------------------------------------------------

    private void loadWildernessPortalFromDB() {
        var db = GameManager.getInstance().getDatabase().system();
        String worldName = db.getServerData(DB_KEY_WORLD);
        String sMinX = db.getServerData(DB_KEY_MIN_X);
        String sMinY = db.getServerData(DB_KEY_MIN_Y);
        String sMinZ = db.getServerData(DB_KEY_MIN_Z);
        String sMaxX = db.getServerData(DB_KEY_MAX_X);
        String sMaxY = db.getServerData(DB_KEY_MAX_Y);
        String sMaxZ = db.getServerData(DB_KEY_MAX_Z);

        if (worldName == null || sMinX == null || sMinY == null || sMinZ == null
                || sMaxX == null || sMaxY == null || sMaxZ == null) {
            LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.WORLD,
                    "Aucun portail Wilderness en DB (premier démarrage ou avant le 1er reset).");
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            LogManager.getInstance().log(Level.WARNING, LogManager.ETagLog.WORLD,
                    "loadWildernessPortalFromDB : monde introuvable → " + worldName);
            return;
        }

        Location min = new Location(world, Integer.parseInt(sMinX), Integer.parseInt(sMinY), Integer.parseInt(sMinZ));
        Location max = new Location(world, Integer.parseInt(sMaxX), Integer.parseInt(sMaxY), Integer.parseInt(sMaxZ));
        portalZones.put(worldName, new Location[]{ min, max });

        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.WORLD,
                "Portail Wilderness chargé depuis DB : " + worldName + " " + fmt(min) + " → " + fmt(max));
    }

    private void saveWildernessPortalToDB(String worldName, Location min, Location max) {
        var db = GameManager.getInstance().getDatabase().system();
        db.saveServerData(DB_KEY_WORLD, worldName);
        db.saveServerData(DB_KEY_MIN_X, String.valueOf(min.getBlockX()));
        db.saveServerData(DB_KEY_MIN_Y, String.valueOf(min.getBlockY()));
        db.saveServerData(DB_KEY_MIN_Z, String.valueOf(min.getBlockZ()));
        db.saveServerData(DB_KEY_MAX_X, String.valueOf(max.getBlockX()));
        db.saveServerData(DB_KEY_MAX_Y, String.valueOf(max.getBlockY()));
        db.saveServerData(DB_KEY_MAX_Z, String.valueOf(max.getBlockZ()));
    }

    private String fmt(Location l) {
        return "(" + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ() + ")";
    }
}