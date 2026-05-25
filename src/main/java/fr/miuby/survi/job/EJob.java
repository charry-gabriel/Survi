package fr.miuby.survi.job;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Enumération de tous les métiers disponibles dans le jeu.
 *
 * Contrairement au {@link fr.miuby.survi.role.ERole} (attribué manuellement par un admin),
 * chaque joueur possède TOUS les métiers simultanément avec un niveau individuel.
 * Le niveau d'un métier évolue grâce à la réputation accumulée auprès du Trader associé.
 *
 * Niveaux 0–10 (voir config.yml → jobs.levels).
 * Effets par niveau :
 *   MINEUR    — multiplicateur de drops sur les minerais  (×0,2 au niv.0 → ×2,0 au niv.10)
 *   BUCHERON  — multiplicateur de drops sur les bûches    (×0,2 au niv.0 → ×2,0 au niv.10)
 *   ENCHANTEUR — plafond XP table d'enchantement (niv×3) et niveau max d'enchantement (= niv)
 */
@Getter
public enum EJob {

    MINEUR        ("Mineur",        NamedTextColor.GRAY),
    BUCHERON      ("Bûcheron",      NamedTextColor.DARK_GREEN),
    FERMIER       ("Fermier",       NamedTextColor.GREEN),
    COMBATANT     ("Combatant",     NamedTextColor.RED),
    ALCHIMISTE    ("Alchimiste",    NamedTextColor.DARK_PURPLE),
    ENCHANTEUR    ("Enchanteur",    NamedTextColor.BLUE),
    FORGERON      ("Forgeron",      NamedTextColor.DARK_GRAY),
    PECHEUR       ("Pêcheur",       NamedTextColor.AQUA),
    CHASSEUR      ("Chasseur",      NamedTextColor.GOLD),
    MARCHAND      ("Marchand",      NamedTextColor.YELLOW),
    AVENTURIER    ("Aventurier",    NamedTextColor.LIGHT_PURPLE),
    BATISSEUR     ("Bâtisseur",     NamedTextColor.WHITE);

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
