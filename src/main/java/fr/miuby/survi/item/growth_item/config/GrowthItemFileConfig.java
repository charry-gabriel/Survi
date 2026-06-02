package fr.miuby.survi.item.growth_item.config;

import java.util.ArrayList;
import java.util.List;

/**
 * POJO SnakeYAML pour les fichiers {@code growth_items/<id>.yml}.
 *
 * <p>Cette classe est la représentation <em>brute</em> du YAML. Elle est convertie
 * en {@link GrowthConfig} (type runtime) par {@link fr.miuby.survi.item.growth_item.GrowthItemLoader}.
 *
 * <p>Toutes les classes internes sont {@code public static} pour que SnakeYAML
 * puisse les instancier via réflexion.
 */
public class GrowthItemFileConfig {

    /** Identifiant correspondant à l'entrée {@code ECustomItem} (ex. {@code GROWTH_CASQUE_MINEUR}). */
    public String id;

    /**
     * Type d'événement qui incrémente les uses (ex. {@code OreBreakEvent},
     * {@code CropBreakEvent}, {@code BlockBreakEvent}).
     */
    public String eventType;

    /** Paliers uniques — chaque palier est déclenché une seule fois quand l'usage seuil est atteint. */
    public List<TierConfig> tiers = new ArrayList<>();

    /** Effets répétés tous les N uses. */
    public List<PeriodicConfig> periodicEffects = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────

    public static class TierConfig {
        /** Nombre de uses accumulés nécessaires pour déclencher ce palier. */
        public int requiredUses;
        public List<EffectConfig> effects = new ArrayList<>();
    }

    public static class PeriodicConfig {
        /** L'effet se déclenche tous les {@code everyUses} uses. */
        public int everyUses;
        public List<EffectConfig> effects = new ArrayList<>();
    }

    /**
     * Représente un effet YAML.
     *
     * <p>Le champ {@code type} détermine quels autres champs sont lus :
     * <ul>
     *   <li>{@code name}            — {@code value} (String)</li>
     *   <li>{@code message}         — {@code value} (String)</li>
     *   <li>{@code haste}           — {@code seconds} (int)</li>
     *   <li>{@code add_enchantment} — {@code enchantment} (clé minecraft, ex. {@code fortune}),
     *                                  {@code amount} (int)</li>
     *   <li>{@code set_attribute}   — {@code attribute} (ex. {@code mining_efficiency}),
     *                                  {@code attributeValue} (double),
     *                                  {@code operation} ({@code ADD_NUMBER} | {@code ADD_SCALAR}),
     *                                  {@code slot} (ex. {@code HEAD})</li>
     * </ul>
     */
    public static class EffectConfig {
        public String type;

        // name / message
        public String value;

        // haste
        public int seconds;

        // add_enchantment
        public String enchantment;
        public int amount;

        // set_attribute
        public String attribute;
        public double attributeValue;
        public String operation;
        public String slot;
    }
}