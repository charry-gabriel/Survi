package fr.miuby.survi.job;

import fr.miuby.survi.player.role.ERole;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Enumération de tous les métiers disponibles dans le jeu.
 *
 * Contrairement au {@link ERole} (attribué manuellement par un admin),
 * chaque joueur possède TOUS les métiers simultanément avec un niveau individuel.
 * Le niveau d'un métier évolue grâce à la réputation accumulée auprès du Trader associé.
 *
 * Niveaux 0–10 (voir config.yml → jobs.levels).
 * Effets par niveau :
 *   MINER      — multiplicateur de drops sur les minerais  (×0,2 au niv.0 → ×2,0 au niv.10)
 *   LUMBERJACK — multiplicateur de drops sur les bûches    (×0,2 au niv.0 → ×2,0 au niv.10)
 *   ENCHANTER  — plafond XP table d'enchantement (niv×3) et niveau max d'enchantement (= niv)
 */
@Getter
public enum EJob {

    MINER      ("Mineur",     NamedTextColor.GRAY),
    LUMBERJACK ("Bûcheron",   NamedTextColor.DARK_GREEN),
    FARMER     ("Fermier",    NamedTextColor.YELLOW),
    ENCHANTER  ("Enchanteur", NamedTextColor.DARK_PURPLE),
    FISHERMAN  ("Pêcheur",    NamedTextColor.BLUE),
    EXPLORER   ("Explorateur", NamedTextColor.RED);

    private final String displayName;
    private final NamedTextColor color;

    EJob(String displayName, NamedTextColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public NamedTextColor getColor() {
        return color;
    }

    public TextComponent toComponent() {
        return Component.text(displayName).color(color);
    }
}
