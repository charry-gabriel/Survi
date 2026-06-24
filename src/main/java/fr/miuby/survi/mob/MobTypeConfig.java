package fr.miuby.survi.mob;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration complète d'un type de mob.
 *
 * <p>Contient :
 * <ul>
 *   <li>Les stats classiques scalées exponentiellement par palier ({@link TieredExpStat})</li>
 *   <li>Le rayon d'explosion du Creeper ({@code explosionRadius}, formule linéaire)</li>
 *   <li>La durée de mèche du Creeper ({@code fuseTicks})</li>
 *   <li>Les effets de potion à l'attaque ({@code potionEffects})</li>
 * </ul>
 *
 * <h3>Formule exponentielle par palier</h3>
 * <pre>
 *   tier   = (level - 1) / levelsPerTier
 *   offset = (level - 1) mod levelsPerTier
 *   t      = offset / (levelsPerTier - 1)   [0.0 → 1.0]
 *
 *   tierBase = base × tierMultiplier^tier
 *   valeur   = tierBase × (1 + (peakRatio - 1) × t^exponent)
 * </pre>
 * Avec les valeurs par défaut ({@code tierMultiplier=2.0, exponent=3.0, levelsPerTier=10}) :
 * <ul>
 *   <li>Niveau 1  → {@code base}             (début palier 0)</li>
 *   <li>Niveau 10 → {@code base × peakRatio} (pic palier 0)</li>
 *   <li>Niveau 11 → {@code base × 2.0}       (reset doux — ≈ niveau 5–6)</li>
 *   <li>Niveau 20 → {@code base × 2.0 × peakRatio} (pic palier 1)</li>
 *   <li>Niveau 30 → {@code base × 4.0 × peakRatio} (boss)</li>
 * </ul>
 */
public class MobTypeConfig {

    // ─── Inner types ──────────────────────────────────────────────────────────────

    /**
     * Stat scalée exponentiellement par palier.
     *
     * @param base            valeur au niveau 1
     * @param peakRatio       multiplicateur de la stat au pic de chaque palier
     *                        ({@code < 1} = la stat diminue, ex. taille du Creeper)
     * @param tierMultiplier  multiplicateur appliqué à la base à chaque nouveau palier
     *                        ({@code > 1} = monte, {@code < 1} = descend)
     * @param exponent        courbure de la croissance dans le palier (3.0 = cubique)
     * @param levelsPerTier   nombre de niveaux par palier (lu dans monsters.yml)
     */
    public record TieredExpStat(double base, double peakRatio,
                                double tierMultiplier, double exponent,
                                int levelsPerTier) {
        public double compute(int level) {
            if (levelsPerTier <= 1) return base * tierMultiplier * peakRatio;
            int    tier   = (level - 1) / levelsPerTier;
            int    offset = (level - 1) % levelsPerTier;
            double t      = (double) offset / (levelsPerTier - 1);
            double tierBase = base * Math.pow(tierMultiplier, tier);
            return tierBase * (1.0 + (peakRatio - 1.0) * Math.pow(t, exponent));
        }
    }

    /**
     * Stat linéaire pour les mécaniques spéciales (rayon d'explosion, etc.).
     * Formule : {@code valeur = base + (level - 1) * perLevel}.
     *
     * @param base     valeur au niveau 1
     * @param perLevel incrément par niveau (peut être négatif)
     */
    public record LinearStat(double base, double perLevel) {
        public double compute(int level) {
            return base + (level - 1) * perLevel;
        }
    }

    /**
     * Durée de mèche du Creeper avec plancher de sécurité.
     * Formule : {@code max(min, base + (level - 1) * perLevel)}
     *
     * @param base     durée de base en ticks (vanilla = 30)
     * @param perLevel variation par niveau (négatif = explosion plus rapide)
     * @param min      durée minimale en ticks
     */
    public record FuseStat(double base, double perLevel, int min) {
        public double compute(int level) {
            return Math.max(min, base + (level - 1) * perLevel);
        }
    }

    // ─── Champs ───────────────────────────────────────────────────────────────────

    private final Map<EMobStat, TieredExpStat>   stats         = new EnumMap<>(EMobStat.class);
    @Getter
    private final List<MobPotionEffectConfig>    potionEffects = new ArrayList<>();
    @Setter
    @Getter
    private LinearStat                           explosionRadius = null;
    @Getter
    private FuseStat                             fuseTicks       = null;

    // ─── Mutateurs ────────────────────────────────────────────────────────────────

    public void addStat(EMobStat stat, TieredExpStat config) {
        stats.put(stat, config);
    }

    /**
     * Configure la durée de mèche du Creeper.
     *
     * @param base     durée de base en ticks (vanilla = 30)
     * @param perLevel variation par niveau (négatif = plus court à haut niveau)
     * @param min      durée minimale en ticks (plancher de sécurité)
     */
    public void setFuseTicks(double base, double perLevel, int min) {
        this.fuseTicks = new FuseStat(base, perLevel, min);
    }

    public void addPotionEffect(MobPotionEffectConfig effect) {
        potionEffects.add(effect);
    }

    // ─── Accesseurs ───────────────────────────────────────────────────────────────

    /**
     * @return valeur calculée pour le niveau donné, ou {@code -1} si la stat est absente.
     */
    public double getStatValue(EMobStat stat, int level) {
        TieredExpStat config = stats.get(stat);
        if (config == null) return -1;
        return config.compute(level);
    }
}