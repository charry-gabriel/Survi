package fr.miuby.survi.item.growth_item.config;

import fr.miuby.survi.item.growth_item.GrowthTier;
import fr.miuby.survi.item.growth_item.effect.ItemEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * Regroupe la configuration complète d'un Growth Item :
 * effets de base, paliers uniques et effets périodiques.
 *
 * <p>{@link #baseEffects()} contient les {@link ItemEffect} appliqués dès la création
 * de l'item et réappliqués en premier lors d'un {@code reapplyAll} (avant les paliers).
 * Ils permettent de rendre les stats initiales configurables via YAML sans recompiler.</p>
 */
public record GrowthConfig(
        String eventType,
        List<ItemEffect> baseEffects,
        List<GrowthTier> tiers,
        List<PeriodicEffect> periodicEffects
) {

    public static Builder builder(String eventType) {
        return new Builder(eventType);
    }

    public record PeriodicEffect(int everyUses, List<ItemEffect> effects) {}

    public static class Builder {
        private final String eventType;
        private List<ItemEffect> baseEffects = new ArrayList<>();
        private final List<GrowthTier> tiers = new ArrayList<>();
        private final List<PeriodicEffect> periodicEffects = new ArrayList<>();

        private Builder(String eventType) {
            this.eventType = eventType;
        }

        /** Effets de base (stats initiales, avant tout palier). */
        public Builder baseEffects(List<ItemEffect> effects) {
            this.baseEffects = List.copyOf(effects);
            return this;
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
            return new GrowthConfig(eventType, List.copyOf(baseEffects), List.copyOf(tiers), List.copyOf(periodicEffects));
        }
    }
}