package fr.miuby.survi.utils;

public class Rect {
    public final int left;
    public final int right;
    public final int top;
    public final int bottom;

    public Rect(int left, int right, int top, int bottom) {
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
    }

    public boolean isOut(int x, int y) {
        return x < left || x > right || y > top || y < bottom;
    }
}
