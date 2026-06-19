package fr.miuby.survi.world;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.world.config.VillageZoneConfig;
import fr.miuby.survi.world.zone.VillageZoneManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Affiche le portail du village comme un waypoint sur la Locator Bar (1.21.6+).
 *
 * <p>Spawn un ArmorStand invisible au centre du portail avec l'attribut
 * {@code WAYPOINT_TRANSMIT_RANGE}, puis lui applique :</p>
 * <ul>
 *   <li>le style {@code survi:portal} (resource pack) — icône portail à la place du point coloré ;</li>
 *   <li>la couleur blanche ({@code hex FFFFFF}) pour afficher la texture sans teinte.</li>
 * </ul>
 *
 * <p>Les points des joueurs sont gérés nativement par Minecraft — ce manager
 * ne les touche pas.</p>
 *
 * <p>Le marker est recréé à chaque changement de palier de zone et nettoyé
 * à l'arrêt du serveur. Un tag PDC {@code survi:portal_locator} permet de
 * retrouver et supprimer d'éventuels résidus entre sessions.</p>
 *
 * <p><b>Persistance UUID :</b> l'UUID du marker est sauvegardé en DB
 * ({@code portal_locator_uuid}) pour survivre aux redémarrages et aux
 * crashs serveur. À l'init, l'UUID est rechargé et l'entité supprimée
 * avant qu'un nouveau marker soit spawné.</p>
 *
 * <p><b>Pré-requis :</b> le resource pack {@code Survi_RP} doit être chargé
 * par les clients, et la gamerule {@code locatorBar} doit être {@code true}.</p>
 */
public class PortalLocatorManager {

    private static final NamespacedKey MARKER_KEY = new NamespacedKey("survi", "portal_locator");
    private static final double WAYPOINT_RANGE = 10_000.0;

    private static final String ENTITY_TAG = "survi_portal_locator";

    /** Style défini dans assets/survi/waypoint_style/portal.json du resource pack. */
    private static final String WAYPOINT_STYLE = "survi:portal";

    /**
     * Clé DB pour persister l'UUID du marker entre redémarrages/crashs.
     * Permet à {@link #removeMarker()} de retrouver l'entité même si
     * {@code markerUUID} a été perdu (redémarrage, crash).
     */
    private static final String DB_KEY_UUID = "portal_locator_uuid";

    private UUID markerUUID = null;

    /** Emplacement du marker actif, pour forcer le chargement de son chunk avant suppression. */
    private Location markerLocation = null;

    /**
     * Chunk gardé en mémoire en continu pour le marker actif (ticket de chunk).
     * Indispensable : un ArmorStand dans un chunk déchargé n'existe plus côté
     * serveur et ne peut transmettre aucun waypoint, même avec un range élevé.
     */
    private World tickedWorld = null;
    private int tickedChunkX;
    private int tickedChunkZ;

    // ── Init / Stop ──────────────────────────────────────────────────────────

    public void init() {
        loadMarkerUUIDFromDB();

        String villageName = WorldInitializer.getWorlds().get(EWorld.VILLAGE);
        if (villageName != null) {
            World world = Bukkit.getWorld(villageName);
            if (world != null) cleanupOldMarkers(world);
        }

        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD, "[PortalLocatorManager] Initialisé (uuid DB=" + markerUUID + ").");
    }

    public void stop() {
        removeMarker();
    }

    // ── API ──────────────────────────────────────────────────────────────────

    /**
     * Appelé par {@link VillageZoneManager} à chaque changement de palier.
     * Force le chargement du chunk de l'ancien marker et de celui du nouveau
     * portail, supprime l'ancien marker puis place le nouveau au centre du
     * portail du palier actif.
     */
    public void updatePortal(World world, VillageZoneConfig.VillageZonePortal portalCfg) {
        Location center = computeCenter(world, portalCfg);

        if (markerLocation != null) loadChunkOf(markerLocation);
        loadChunkOf(center);

        removeMarker();
        spawnMarker(center);
    }

    /** Force le chargement du chunk d'une location si nécessaire (entité non trackée sinon). */
    private void loadChunkOf(Location location) {
        World w = location.getWorld();
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        if (!w.isChunkLoaded(chunkX, chunkZ)) {
            w.loadChunk(chunkX, chunkZ);
            MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                    "[PortalLocatorManager] Chunk (" + chunkX + "," + chunkZ + ") forcé.");
        }
    }

    /** Pose un ticket de chunk plugin pour garder le chunk du marker chargé en permanence. */
    private void addChunkTicket(Location location) {
        World world = location.getWorld();
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        world.addPluginChunkTicket(chunkX, chunkZ, GameManager.getInstance().getPlugin());
        tickedWorld = world;
        tickedChunkX = chunkX;
        tickedChunkZ = chunkZ;
    }

    /** Libère le ticket de chunk posé par {@link #addChunkTicket(Location)}, le cas échéant. */
    private void removeChunkTicket() {
        if (tickedWorld == null) return;
        tickedWorld.removePluginChunkTicket(tickedChunkX, tickedChunkZ, GameManager.getInstance().getPlugin());
        tickedWorld = null;
    }

    // ── Interne ──────────────────────────────────────────────────────────────

    private void spawnMarker(Location location) {
        addChunkTicket(location);

        ArmorStand stand = location.getWorld().spawn(location, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setSmall(true);
            as.setSilent(true);
            as.setInvulnerable(true);
            as.setPersistent(true);
            as.setMarker(true);
            as.addScoreboardTag(ENTITY_TAG);
            as.getPersistentDataContainer().set(MARKER_KEY, PersistentDataType.BYTE, (byte) 1);
        });

        markerUUID = stand.getUniqueId();
        markerLocation = location;
        saveMarkerUUIDToDB(); // ← persiste l'UUID pour les redémarrages/crashs

        final UUID uuid = markerUUID;
        // L'attribut WAYPOINT_TRANSMIT_RANGE doit être posé après le spawn, pas dedans :
        // Mojang MC-296583 — posé au moment du summon, l'entité ne se transmet pas
        // correctement sur la Locator Bar (visible seulement à courte portée).
        Bukkit.getScheduler().runTaskLater(GameManager.getInstance().getPlugin(), () ->
                applyWaypointTransmitRange(uuid), 1L);

        MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                "[PortalLocatorManager] Marker portail spawné en " + fmt(location) + " (uuid=" + uuid + ")");
    }

    /**
     * Pose l'attribut {@code WAYPOINT_TRANSMIT_RANGE} puis applique le style et la couleur
     * du waypoint portail via les commandes vanilla. Exécuté 1 tick après le spawn.
     *
     * <ul>
     *   <li>{@code WAYPOINT_TRANSMIT_RANGE} — portée de transmission du waypoint ;</li>
     *   <li>{@code style survi:portal} — icône définie dans le resource pack ;</li>
     *   <li>{@code color hex FFFFFF} — aucune teinte, la texture s'affiche dans ses propres couleurs.</li>
     * </ul>
     */
    private void applyWaypointTransmitRange(UUID uuid) {
        Entity entity = Bukkit.getEntity(uuid);
        if (entity instanceof ArmorStand stand) {
            AttributeInstance attr = stand.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE);
            if (attr != null) attr.setBaseValue(WAYPOINT_RANGE);
        }

        String selector = "@e[type=armor_stand,tag=" + ENTITY_TAG + ",limit=1]";

        boolean styleOk = Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "waypoint modify " + selector + " style set " + WAYPOINT_STYLE);
        boolean colorOk = Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                "waypoint modify " + selector + " color hex FFFFFF");

        if (!styleOk || !colorOk) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.WORLD,
                    "[PortalLocatorManager] /waypoint modify a échoué (style=" + styleOk
                            + " color=" + colorOk + "). "
                            + "Vérifier que locatorBar=true et que le resource pack Survi_RP est chargé.");
        } else {
            MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                    "[PortalLocatorManager] Style portail appliqué (uuid=" + uuid + ")");
        }
    }

    private void removeMarker() {
        if (markerUUID == null) return;

        boolean found = false;
        for (World w : Bukkit.getWorlds()) {
            Entity e = w.getEntity(markerUUID);
            if (e != null) {
                e.remove();
                found = true;
                break;
            }
        }

        if (!found) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.WORLD,
                    "[PortalLocatorManager] Marker uuid=" + markerUUID + " introuvable par UUID, fallback scan PDC.");
            for (World w : Bukkit.getWorlds()) {
                cleanupOldMarkers(w);
            }
        }

        markerUUID = null;
        markerLocation = null;
        removeChunkTicket();
        clearMarkerUUIDInDB(); // ← efface l'UUID en DB (arrêt propre)
    }

    private void cleanupOldMarkers(World world) {
        world.getEntitiesByClass(ArmorStand.class).stream()
                .filter(e -> e.getPersistentDataContainer().has(MARKER_KEY, PersistentDataType.BYTE))
                .forEach(Entity::remove);
    }

    // ── Persistance DB ──────────────────────────────────────────────────────

    private void loadMarkerUUIDFromDB() {
        String stored = GameManager.getInstance().getDatabase().system().getServerData(DB_KEY_UUID);
        if (stored == null || stored.isBlank()) return;
        try {
            markerUUID = UUID.fromString(stored);
            MLLogManager.getInstance().log(Level.INFO, ELogTag.WORLD,
                    "[PortalLocatorManager] UUID chargé depuis DB : " + markerUUID);
        } catch (IllegalArgumentException e) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.WORLD,
                    "[PortalLocatorManager] UUID DB invalide, ignoré : " + stored);
        }
    }

    private void saveMarkerUUIDToDB() {
        GameManager.getInstance().getDatabase().system()
                .saveServerData(DB_KEY_UUID, markerUUID.toString());
    }

    /**
     * Efface l'UUID en DB après un arrêt propre du serveur.
     * Utilise une chaîne vide (SystemRepository n'expose pas de méthode delete).
     */
    private void clearMarkerUUIDInDB() {
        GameManager.getInstance().getDatabase().system()
                .saveServerData(DB_KEY_UUID, "");
    }

    // ── Utilitaires ─────────────────────────────────────────────────────────

    /** Centre XZ de la zone portail, Y au bas du portail + 1 pour être dans l'ouverture. */
    private Location computeCenter(World world, VillageZoneConfig.VillageZonePortal cfg) {
        return new Location(world,
                (cfg.minX() + cfg.maxX()) / 2.0 + 0.5,
                cfg.minY() + 1.0,
                (cfg.minZ() + cfg.maxZ()) / 2.0 + 0.5);
    }

    private String fmt(Location l) {
        return "(" + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ() + ")";
    }
}