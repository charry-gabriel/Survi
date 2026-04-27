package fr.miuby.survi.job;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Enumération de tous les métiers disponibles dans le jeu.
 *
 * Contrairement au {@link fr.miuby.survi.role.ERole} (attribué manuellement par un admin),
 * chaque joueur possède TOUS les métiers simultanément avec un niveau individuel.
 * Le niveau d'un métier évolue grâce à la réputation accumulée auprès du Trader associé.
 */
public enum EJob {

    MINEUR        ("Mineur",        NamedTextColor.GRAY),
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

    public TextComponent toLevelComponent(int level) {
        String levelLabel = JobLevelConfig.getLevelName(level);
        return Component.text(displayName + " — " + levelLabel + " (niv." + level + ")")
                .color(color);
    }
}