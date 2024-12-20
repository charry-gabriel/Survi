package fr.miuby.survi.utils;

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

    public boolean isOut(int x, int y, int z) {
        return x > xMax || x < xMin || y > yMax || y < yMin || z > zMax || z < zMin;
    }
}
