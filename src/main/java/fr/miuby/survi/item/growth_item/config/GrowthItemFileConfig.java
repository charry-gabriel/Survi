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

    /**
     * Effets appliqués dès la création de l'item <em>et</em> réappliqués à chaque reload
     * (avant les effets de palier).
     *
     * <p>Permet de modifier les stats de base d'un growth item via YAML sans recompiler.
     * Typiquement des {@code set_attribute} (ex. {@code mining_efficiency: 3.0} sur le casque)
     * ou des {@code add_enchantment} initiaux.</p>
     *
     * <p>Optionnel — peut être absent du YAML (SnakeYAML laisse la liste vide).</p>
     */
    public List<EffectConfig> baseEffects = new ArrayList<>();

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

    public static class EffectConfig {
        public String type;
        public String value;
        public int seconds;
        public String effect;
        public int amplifier;
        public String enchantment;
        public int amount;
        public String attribute;
        public double attributeValue;
        public String operation;
        public String slot;
    }
}