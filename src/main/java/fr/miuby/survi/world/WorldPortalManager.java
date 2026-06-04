package fr.miuby.survi.world;

import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.system.log.ELogTag;
import org.bukkit.*;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.logging.Level;

public class WorldPortalManager {

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

        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD, "WorldPortalManager initialisé.");
    }

    // -------------------------------------------------------------------------
    // Enregistrement du portail Wilderness (appelé par WorldResetManager)
    // -------------------------------------------------------------------------

    /**
     * Enregistre la zone intérieure (blocs de verre) du portail Wilderness.
     * Sauvegarde min et max en DB, puis envoie les faux blocs directement aux
     * joueurs présents (leurs chunks sont déjà chargés côté client puisqu'ils
     * sont déjà dans le monde). Les nouveaux arrivants reçoivent les faux blocs
     * via {@link #sendFakePortalBlocksInChunk}, déclenché par PlayerChunkLoadEvent.
     */
    public void registerWildernessPortal(String worldName, Location min, Location max) {
        portalZones.put(worldName, new Location[]{ min.clone(), max.clone() });
        saveWildernessPortalToDB(worldName, min, max);

        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            world.getPlayers().forEach(p -> sendFakePortalBlocks(p, min, max));
        }

        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                "Portail Wilderness enregistré : " + worldName + " " + fmt(min) + " → " + fmt(max) + " (axe : " + detectAxis(min, max) + ")");
    }

    /**
     * Met à jour la position du portail Village (appelé par {@link VillageZoneManager}
     * à chaque changement de palier de zone).
     *
     * <p>Efface les anciens faux blocs NETHER_PORTAL et envoie les nouveaux directement
     * aux joueurs présents dans le Village (chunks déjà chargés côté client).</p>
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
            world.getPlayers().forEach(p -> sendFakePortalBlocks(p, min, max));
        }

        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                "Portail Village mis à jour : " + fmt(min) + " → " + fmt(max) + " (axe : " + detectAxis(min, max) + ")");
    }

    /** Retire la zone du portail Wilderness avant unload (reset). */
    public void unregisterWildernessPortal(String worldName) {
        if (portalZones.remove(worldName) != null) {
            MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD, "Portail Wilderness désenregistré : " + worldName);
        }
    }

    // -------------------------------------------------------------------------
    // Envoi par chunk — déclenché par PlayerChunkLoadEvent dans WorldListener
    // -------------------------------------------------------------------------

    /**
     * Indique si le chunk (chunkX, chunkZ) contient des blocs du portail enregistré
     * pour ce monde. Appel O(1) — utilisé par WorldListener pour filtrer les chunks
     * sans portail avant de planifier un envoi.
     */
    public boolean chunkOverlapsPortal(String worldName, int chunkX, int chunkZ) {
        Location[] zone = portalZones.get(worldName);
        if (zone == null) return false;
        int minCX = Math.min(zone[0].getBlockX(), zone[1].getBlockX()) >> 4;
        int maxCX = Math.max(zone[0].getBlockX(), zone[1].getBlockX()) >> 4;
        int minCZ = Math.min(zone[0].getBlockZ(), zone[1].getBlockZ()) >> 4;
        int maxCZ = Math.max(zone[0].getBlockZ(), zone[1].getBlockZ()) >> 4;
        return chunkX >= minCX && chunkX <= maxCX && chunkZ >= minCZ && chunkZ <= maxCZ;
    }

    /**
     * Envoie les faux blocs NETHER_PORTAL appartenant au chunk (chunkX, chunkZ)
     * au joueur donné.
     *
     * <p>Appelé depuis {@link fr.miuby.survi.listener.WorldListener#onPlayerChunkLoad}
     * avec un délai d'1 tick pour garantir que le paquet chunk précède le BlockChange
     * dans la file TCP du client.</p>
     */
    public void sendFakePortalBlocksInChunk(Player player, int chunkX, int chunkZ) {
        Location[] zone = portalZones.get(player.getWorld().getName());
        if (zone == null) return;

        Location min = zone[0];
        Location max = zone[1];
        World world = min.getWorld();
        if (world == null) return;

        Axis axis = detectAxis(min, max);
        var fakePortal = Bukkit.createBlockData(Material.NETHER_PORTAL, bd -> ((Orientable) bd).setAxis(axis));

        iterateZone(min, max, (w, x, y, z) -> {
            if ((x >> 4) == chunkX && (z >> 4) == chunkZ) {
                player.sendBlockChange(new Location(w, x, y, z), fakePortal);
            }
        });
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
            MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
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
            MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
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

    public void teleportToWorld(Player player, Location to, String worldName) {
        Location[] zone = portalZones.get(worldName);
        if (zone == null) return;
        if (!isInPortalZone(to, zone[0], zone[1])) return;
        if (teleporting.contains(player.getUniqueId())) return;

        String villageName = WorldInitializer.getWorlds().get(EWorld.VILLAGE);
        String wildName    = WorldInitializer.getWorlds().get(EWorld.WILDERNESS);
        String netherName  = WorldInitializer.getWorlds().get(EWorld.NETHER);

        if (worldName.equals(villageName)) {
            triggerTeleport(player, EWorld.WILDERNESS);
        } else if (worldName.equals(wildName)) {
            triggerTeleport(player, EWorld.VILLAGE);
        } else if (worldName.equals(netherName)) {
            triggerTeleport(player, EWorld.WILDERNESS);
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

    private void triggerTeleport(Player player, EWorld destination) {
        teleporting.add(player.getUniqueId());

        var mlWorld = WorldRegistry.get(destination);
        if (mlWorld == null) {
            MLLogManager.getInstance().log(Level.SEVERE, ELogTag.WORLD, "triggerTeleport : monde non enregistré → " + destination);
            teleporting.remove(player.getUniqueId());
            return;
        }

        player.teleport(mlWorld.getWorld().getSpawnLocation());

        Bukkit.getScheduler().runTaskLater(GameManager.getInstance().getPlugin(), () -> teleporting.remove(player.getUniqueId()), 20L);
    }

    // -------------------------------------------------------------------------
    // Persistance DB
    // -------------------------------------------------------------------------

    private static final String DB_KEY_WORLD  = "world_portal_wild_world";
    private static final String DB_KEY_MIN_X  = "world_portal_wild_min_x";
    private static final String DB_KEY_MIN_Y  = "world_portal_wild_min_y";
    private static final String DB_KEY_MIN_Z  = "world_portal_wild_min_z";
    private static final String DB_KEY_MAX_X  = "world_portal_wild_max_x";
    private static final String DB_KEY_MAX_Y  = "world_portal_wild_max_y";
    private static final String DB_KEY_MAX_Z  = "world_portal_wild_max_z";

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
            MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                    "Aucun portail Wilderness en DB (premier démarrage ou avant le 1er reset).");
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.WORLD,
                    "loadWildernessPortalFromDB : monde introuvable → " + worldName);
            return;
        }

        Location min = new Location(world, Integer.parseInt(sMinX), Integer.parseInt(sMinY), Integer.parseInt(sMinZ));
        Location max = new Location(world, Integer.parseInt(sMaxX), Integer.parseInt(sMaxY), Integer.parseInt(sMaxZ));
        portalZones.put(worldName, new Location[]{ min, max });

        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
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