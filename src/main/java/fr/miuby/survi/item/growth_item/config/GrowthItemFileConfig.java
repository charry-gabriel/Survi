package fr.miuby.survi.item.growth_item.config;

import java.util.ArrayList;
import java.util.List;

/**
 * POJO SnakeYAML pour les fichiers {@code growth_items/<id>.yml}.
 * Convertie en {@link GrowthConfig} par {@link fr.miuby.survi.item.growth_item.GrowthItemLoader}.
 */
public class GrowthItemFileConfig {

    public String id;
    public String eventType;
    public List<TierConfig> tiers = new ArrayList<>();
    public List<PeriodicConfig> periodicEffects = new ArrayList<>();

    public static class TierConfig {
        public int requiredUses;
        public List<EffectConfig> effects = new ArrayList<>();
    }

    public static class PeriodicConfig {
        public int everyUses;
        public List<EffectConfig> effects = new ArrayList<>();
    }

    /**
     * Un effet YAML. Le champ {@code type} détermine quels autres champs sont lus :
     *
     * <ul>
     *   <li>{@code name}            — {@code value}</li>
     *   <li>{@code message}         — {@code value}</li>
     *   <li>{@code haste}           — {@code seconds}</li>
     *   <li>{@code potion}          — {@code effect} (clé Bukkit, ex. {@code speed}),
     *                                  {@code seconds}, {@code amplifier} (défaut 0 = niveau I)</li>
     *   <li>{@code add_enchantment} — {@code enchantment}, {@code amount}</li>
     *   <li>{@code set_attribute}   — {@code attribute}, {@code attributeValue},
     *                                  {@code operation}, {@code slot}</li>
     * </ul>
     */
    public static class EffectConfig {
        public String type;

        // name / message
        public String value;

        // haste / potion
        public int seconds;

        // potion
        public String effect;    // PotionEffectType en minuscule (ex. speed, strength, night_vision)
        public int amplifier;    // 0 = niveau I, 1 = niveau II — défaut SnakeYAML : 0

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