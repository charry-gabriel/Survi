package fr.miuby.survi.world.config;

import java.util.List;

/** Configuration complète de la zone village, chargée depuis zone.yml. */
public record VillageZoneConfig(List<VillageZoneStage> stages) {

    /** Un palier de zone village. */
    public record VillageZoneStage(float afterHours, int centerX, int centerZ, int halfWidth, int halfDepth, VillageZoneSpawn spawn, VillageZonePortal portal) {}

    /** Point de spawn du village pour un palier donné. */
    public record VillageZoneSpawn(float x, float y, float z, float yaw, float pitch) {}

    /** Coordonnées du portail village pour un palier donné. */
    public record VillageZonePortal(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {}
}