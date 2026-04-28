package fr.miuby.survi.player;

import fr.miuby.survi.system.SurviConfig;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Rang global basé sur la réputation totale de tous les Traders.
 *
 * <p>Les seuils ({@link #threshold}) et noms ({@link #displayName}) sont chargés
 * depuis le {@code config.yml} via {@link #initFromConfig(SurviConfig)}.
 * Les couleurs, elles, restent définies dans l'enum car elles relèvent du code.
 *
 * <p>L'{@code id} dans le config.yml doit correspondre exactement au nom de l'entrée
 * d'enum (ex : {@code MAITRE} → {@link #MAITRE}).
 */
@Getter
public enum GlobalRank {

    INCONNU    (NamedTextColor.DARK_GRAY),
    NOVICE     (NamedTextColor.GRAY),
    APPRENTI   (NamedTextColor.WHITE),
    MARCHAND   (NamedTextColor.GREEN),
    EXPERT     (NamedTextColor.AQUA),
    MAITRE     (NamedTextColor.GOLD),
    LEGENDAIRE (NamedTextColor.LIGHT_PURPLE);

    // Mutable : surchargés par initFromConfig()
    private String displayName;
    private int    threshold;

    private final NamedTextColor color;

    GlobalRank(NamedTextColor color) {
        // Valeurs par défaut en attendant l'appel à initFromConfig()
        this.displayName = name().charAt(0) + name().substring(1).toLowerCase();
        this.threshold   = 0;
        this.color       = color;
    }

    // ─── Initialisation depuis la config ─────────────────────────────────────────

    /**
     * Applique les seuils et noms d'affichage définis dans {@code config.yml}.
     * Appelé une seule fois par {@link SurviConfig#init}.
     */
    public static void initFromConfig(SurviConfig config) {
        for (SurviConfig.RankEntry entry : config.getRankEntries()) {
            try {
                GlobalRank rank = GlobalRank.valueOf(entry.id());
                rank.displayName = entry.display();
                rank.threshold   = entry.threshold();
            } catch (IllegalArgumentException e) {
                // id inconnu dans l'enum → on ignore (log géré par SurviConfig)
            }
        }
    }

    // ─── Utilitaires ─────────────────────────────────────────────────────────────

    public Component displayComponent() {
        return Component.text("[" + displayName + "]", color);
    }

    /** Retourne le rang correspondant à {@code total} points de réputation. */
    public static GlobalRank fromReputation(int total) {
        GlobalRank result = INCONNU;
        for (GlobalRank rank : values()) {
            if (total >= rank.threshold) result = rank;
        }
        return result;
    }
}
