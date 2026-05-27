package fr.miuby.survi.world.config;

import java.util.List;

/** Configuration complète de la zone village, chargée depuis config.yml. */
public record VillageZoneConfig(int centerX, int centerZ, List<VillageZoneStage> stages) {

    /** Un palier de zone village. */
    public record VillageZoneStage(float afterHours, int radius, VillageZoneSpawn spawn, VillageZonePortal portal) {}

    /** Point de spawn du village pour un palier donné. */
    public record VillageZoneSpawn(int x, int y, int z, float yaw, float pitch) {}

    /** Coordonnées du portail village pour un palier donné. */
    public record VillageZonePortal(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {}
}
