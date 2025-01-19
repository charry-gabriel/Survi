package fr.miuby.survi.utils;

/**
 * The Rect class represents a three-dimensional rectangular boundary defined by
 * its maximum and minimum coordinates along the X, Y, and Z axes. It can be
 * used to determine if a given point is inside or outside the rectangular bounds.
 */
public class Rect {
    public final int xMax;
    public final int xMin;
    public final int yMax;
    public final int yMin;
    public final int zMax;
    public final int zMin;

    public Rect(int xMax, int xMin, int yMax, int yMin, int zMax, int zMin) {
        this.xMax = xMax;
        this.xMin = xMin;
        this.yMax = yMax;
        this.yMin = yMin;
        this.zMax = zMax;
        this.zMin = zMin;
    }

    /**
     * Determines whether a point defined by its X, Y, and Z coordinates lies outside
     * the boundaries of the rectangular area defined by the current instance.
     *
     * @param x the X-coordinate of the point to check
     * @param y the Y-coordinate of the point to check
     * @param z the Z-coordinate of the point to check
     * @return true if the point is outside the boundaries; false otherwise
     */
    public boolean isOut(int x, int y, int z) {
        return x > xMax || x < xMin || y > yMax || y < yMin || z > zMax || z < zMin;
    }
}
