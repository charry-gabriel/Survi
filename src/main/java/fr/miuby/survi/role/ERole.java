package fr.miuby.survi.role;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Enumération de tous les rôles disponibles dans le jeu.
 *
 * Le displayName et la couleur sont définis ici (comme pour EJob).
 * Les attributs de gameplay sont chargés depuis roles.yml par RoleLoader.
 */
@Getter
public enum ERole {

    DRAGON      ("Dragon \uD83D\uDC09",   NamedTextColor.GOLD),
    LOUP_GAROU  ("Loup Garou \uD83D\uDC3A", NamedTextColor.DARK_RED),
    FEE         ("Fée \uD83E\uDDDA",      NamedTextColor.LIGHT_PURPLE),
    NAIN        ("Nain \uD83C\uDF44",     NamedTextColor.DARK_GRAY),
    GEANT       ("Géant \uD83C\uDF44",    NamedTextColor.RED),
    NOVICE      ("❤",                    NamedTextColor.GRAY),
    COMBATANT   ("\uD83D\uDDE1",         NamedTextColor.DARK_RED),
    MINEUR      ("⛏",                   NamedTextColor.GRAY),
    ALCHIMISTE  ("⚗",                   NamedTextColor.DARK_PURPLE),
    ENCHANTEUR  ("\uD83E\uDDD9",         NamedTextColor.AQUA),
    FERMIER     ("\uD83C\uDF3E",         NamedTextColor.GREEN);

    private final String displayName;
    private final NamedTextColor color;

    ERole(String displayName, NamedTextColor color) {
        this.displayName = displayName;
        this.color = color;
    }

    public TextComponent toComponent() {
        return Component.text("[" + displayName + "]").color(color);
    }
}
