package fr.miuby.survi.item.growth_item.config;

import fr.miuby.survi.item.growth_item.GrowthTier;
import fr.miuby.survi.item.growth_item.effect.ItemEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * Regroupe la configuration d'un Growth Item : paliers uniques + effets périodiques.
 */
public record GrowthConfig(String eventType, List<GrowthTier> tiers, List<PeriodicEffect> periodicEffects) {
    
    public static Builder builder(String eventType) {
        return new Builder(eventType);
    }

    public static class Builder {
        private final String eventType;
        private final List<GrowthTier> tiers = new ArrayList<>();
        private final List<PeriodicEffect> periodicEffects = new ArrayList<>();

        private Builder(String eventType) {
            this.eventType = eventType;
        }

        public Builder tier(int requiredUses, ItemEffect... effects) {
            tiers.add(new GrowthTier(requiredUses, List.of(effects)));
            return this;
        }

        public Builder periodic(int everyUses, ItemEffect... effects) {
            periodicEffects.add(new PeriodicEffect(everyUses, List.of(effects)));
            return this;
        }

        public GrowthConfig build() {
            return new GrowthConfig(eventType, tiers, periodicEffects);
        }
    }
}