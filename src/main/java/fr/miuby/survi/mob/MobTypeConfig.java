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
 *   <li>Les stats classiques scalées linéairement ({@link LinearStat})</li>
 *   <li>Le rayon d'explosion du Creeper ({@code explosionRadius})</li>
 *   <li>La durée de mèche du Creeper ({@code fuseTicks})</li>
 *   <li>Les effets de potion à l'attaque ({@code potionEffects})</li>
 * </ul>
 *
 * <h3>Formule linéaire</h3>
 * {@code valeur = base + (level - 1) * perLevel}
 */
public class MobTypeConfig {

    // ─── Inner types ──────────────────────────────────────────────────────────────

    /**
     * Stat scalée linéairement : {@code valeur = base + (level - 1) * perLevel}.
     *
     * @param base     valeur au niveau 1 (≈ vanilla)
     * @param perLevel incrément par niveau supplémentaire (peut être négatif)
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

    private final Map<EMobStat, LinearStat>       stats         = new EnumMap<>(EMobStat.class);
    @Getter
    private final List<MobPotionEffectConfig>    potionEffects = new ArrayList<>();
    @Setter
    @Getter
    private LinearStat                           explosionRadius = null;
    @Getter
    private FuseStat                             fuseTicks       = null;

    // ─── Mutateurs ────────────────────────────────────────────────────────────────

    public void addStat(EMobStat stat, LinearStat config) {
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
        LinearStat config = stats.get(stat);
        if (config == null) return -1;
        return config.compute(level);
    }
}