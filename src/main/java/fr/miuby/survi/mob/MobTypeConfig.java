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
 *   <li>Les stats classiques (via {@link MobStat} / {@link MobStatConfig})</li>
 *   <li>Le rayon d'explosion du Creeper ({@code explosionRadius})</li>
 *   <li>La durée de mèche du Creeper ({@code fuseTicks}) — vitesse d'explosion</li>
 *   <li>Les effets de potion à l'attaque ({@code potionEffects}), ex. araignée</li>
 * </ul>
 */
public class MobTypeConfig {

    @Getter
    private final boolean enabled;
    private final Map<MobStat, MobStatConfig>  stats         = new EnumMap<>(MobStat.class);
    /**
     * -- GETTER --
     * Liste des effets de potion appliqués à l'attaque (peut être vide).
     */
    @Getter
    private final List<MobPotionEffectConfig>   potionEffects = new ArrayList<>();

    /** Rayon d'explosion scalé (Creeper uniquement). Null si non configuré.
     * -- GETTER --
     * Rayon d'explosion du Creeper, ou
     *  si non configuré.
     */
    @Getter
    @Setter
    private MobStatConfig explosionRadius = null;

    /** Durée de mèche scalée (Creeper uniquement). Null si non configuré.
     * -- GETTER --
     * Config de la mèche du Creeper, ou
     *  si non configurée.
     */
    @Getter
    private MobFuseConfig fuseTicks = null;

    public MobTypeConfig(boolean enabled) {
        this.enabled = enabled;
    }

    // ─── Mutateurs ────────────────────────────────────────────────────────────────

    public void addStat(MobStat stat, MobStatConfig config) {
        stats.put(stat, config);
    }

    /**
     * Configure la durée de mèche du Creeper.
     *
     * @param enabled   activé ou non
     * @param base      durée de base en ticks (vanilla = 30)
     * @param perLevel  variation par niveau (négatif = plus court à haut niveau)
     * @param min       durée minimale en ticks (plancher de sécurité)
     */
    public void setFuseTicks(boolean enabled, double base, double perLevel, int min) {
        this.fuseTicks = new MobFuseConfig(enabled, base, perLevel, min);
    }

    public void addPotionEffect(MobPotionEffectConfig effect) {
        potionEffects.add(effect);
    }

    // ─── Accesseurs ───────────────────────────────────────────────────────────────

    /**
     * @return valeur calculée pour le niveau donné, ou {@code -1} si la stat est absente / désactivée.
     */
    public double getStatValue(MobStat stat, int level) {
        MobStatConfig config = stats.get(stat);
        if (config == null) return -1;
        return config.compute(level);
    }
}