package fr.miuby.survi.world.zone;

import org.bukkit.Location;

/**
 * Zone rectangulaire (XZ uniquement) définie par un centre et des demi-dimensions.
 *
 * <p>Utilisée à la fois par le {@link ZoneBorderTask} et le
 * {@code PlayerListener} pour la détection de limite, que la zone soit globale
 * (Village — varie par palier) ou par joueur (Wilderness / Nether — varie par niveau Explorateur).
 *
 * <pre>
 *   minX = centerX - halfWidth   maxX = centerX + halfWidth
 *   minZ = centerZ - halfDepth   maxZ = centerZ + halfDepth
 * </pre>
 */
public record ZoneBounds(int centerX, int centerZ, int halfWidth, int halfDepth) {

    /** {@code true} si la position est hors des limites (contrôle XZ uniquement). */
    public boolean isOutside(Location loc) {
        double dx = Math.abs(loc.getX() - centerX);
        double dz = Math.abs(loc.getZ() - centerZ);
        return dx > halfWidth || dz > halfDepth;
    }

    public int minX() { return centerX - halfWidth; }
    public int maxX() { return centerX + halfWidth; }
    public int minZ() { return centerZ - halfDepth; }
    public int maxZ() { return centerZ + halfDepth; }
}