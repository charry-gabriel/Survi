package fr.miuby.survi.mob;

import lombok.Getter;
import org.bukkit.attribute.Attribute;

/**
 * Chaque valeur correspond à une stat scalable via un {@link Attribute} Bukkit.
 * La clé YAML ({@code configKey}) correspond à la sous-clé dans la section {@code stats}
 * de chaque mob dans {@code monsters.yml}.
 *
 * <p>Stats spéciales NON couvertes ici (traitées séparément dans {@link MobTypeConfig}) :
 * <ul>
 *   <li>{@code explosion-radius} — rayon d'explosion du Creeper (via {@code Creeper#setExplosionRadius})</li>
 *   <li>{@code potion-effects}   — effets de potion appliqués à l'attaque (ex. araignée)</li>
 * </ul>
 */
public enum EMobStat {

    MAX_HEALTH          ("max-health",           Attribute.MAX_HEALTH),
    ATTACK_DAMAGE       ("attack-damage",         Attribute.ATTACK_DAMAGE),
    MOVEMENT_SPEED      ("movement-speed",        Attribute.MOVEMENT_SPEED),
    SCALE               ("scale",                 Attribute.SCALE),
    FOLLOW_RANGE        ("follow-range",          Attribute.FOLLOW_RANGE),
    ARMOR               ("armor",                 Attribute.ARMOR),
    KNOCKBACK_RESISTANCE("knockback-resistance",  Attribute.KNOCKBACK_RESISTANCE);

    @Getter private final String    configKey;
    @Getter private final Attribute attribute;

    EMobStat(String configKey, Attribute attribute) {
        this.configKey = configKey;
        this.attribute = attribute;
    }
}