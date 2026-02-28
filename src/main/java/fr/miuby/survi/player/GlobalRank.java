package fr.miuby.survi.player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Rang global basé sur la réputation totale de tous les Traders.
 * Ajustez les seuils selon votre équilibrage.
 */
public enum GlobalRank {

    INCONNU    ("Inconnu",    0,    NamedTextColor.DARK_GRAY),
    NOVICE     ("Novice",     50,   NamedTextColor.GRAY),
    APPRENTI   ("Apprenti",   150,  NamedTextColor.WHITE),
    MARCHAND   ("Marchand",   350,  NamedTextColor.GREEN),
    EXPERT     ("Expert",     700,  NamedTextColor.AQUA),
    MAITRE     ("Maître",     1200, NamedTextColor.GOLD),
    LEGENDAIRE ("Légendaire", 2000, NamedTextColor.LIGHT_PURPLE);

    private final String displayName;
    private final int threshold;
    private final NamedTextColor color;

    GlobalRank(String displayName, int threshold, NamedTextColor color) {
        this.displayName = displayName;
        this.threshold   = threshold;
        this.color       = color;
    }

    public Component displayComponent() {
        return Component.text("[" + displayName + "]", color);
    }

    public String getDisplayName() { return displayName; }
    public int getThreshold()      { return threshold; }
    public NamedTextColor getColor() { return color; }

    public static GlobalRank fromReputation(int total) {
        GlobalRank result = INCONNU;
        for (GlobalRank rank : values()) {
            if (total >= rank.threshold) result = rank;
        }
        return result;
    }
}