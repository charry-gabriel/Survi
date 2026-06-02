package fr.miuby.survi.item.growth_item.config;

import fr.miuby.survi.item.growth_item.GrowthTier;
import fr.miuby.survi.item.growth_item.effect.ItemEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * Regroupe la configuration d'un Growth Item : paliers uniques + effets périodiques.
 *
 * <p>Les deux surcharges de {@link Builder#tier} et {@link Builder#periodic}
 * (varargs et {@link List}) permettent au code Java de rester lisible
 * et au {@link fr.miuby.survi.item.growth_item.GrowthItemLoader} d'éviter un {@code toArray()}.
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

        /** Utilisé par le code Java direct (lambdas, inline). */
        public Builder tier(int requiredUses, ItemEffect... effects) {
            tiers.add(new GrowthTier(requiredUses, List.of(effects)));
            return this;
        }

        /** Utilisé par {@link fr.miuby.survi.item.growth_item.GrowthItemLoader}. */
        public Builder tier(int requiredUses, List<ItemEffect> effects) {
            tiers.add(new GrowthTier(requiredUses, List.copyOf(effects)));
            return this;
        }

        /** Utilisé par le code Java direct. */
        public Builder periodic(int everyUses, ItemEffect... effects) {
            periodicEffects.add(new PeriodicEffect(everyUses, List.of(effects)));
            return this;
        }

        /** Utilisé par {@link fr.miuby.survi.item.growth_item.GrowthItemLoader}. */
        public Builder periodic(int everyUses, List<ItemEffect> effects) {
            periodicEffects.add(new PeriodicEffect(everyUses, List.copyOf(effects)));
            return this;
        }

        public GrowthConfig build() {
            return new GrowthConfig(eventType, tiers, periodicEffects);
        }
    }
}