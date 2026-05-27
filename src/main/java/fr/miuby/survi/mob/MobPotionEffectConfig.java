package fr.miuby.survi.mob;

import org.bukkit.potion.PotionEffectType;

/**
 * Configuration d'un effet de potion appliqué lors de l'attaque d'un mob (ex. araignée).
 *
 * <p>Toutes les valeurs évoluent linéairement avec le niveau du mob :
 * <ul>
 *   <li><b>chance</b>    : {@code min(maxChance, chanceBase + (level - minMobLevel) * chancePerLevel)}</li>
 *   <li><b>durée</b>     : {@code durationBase + (level - minMobLevel) * durationPerLevel} ticks</li>
 *   <li><b>amplifier</b> : {@code (level - minMobLevel) / amplifierUpgradeEvery} (0 = niveau I, 1 = II…)</li>
 * </ul>
 */
public record MobPotionEffectConfig(
        PotionEffectType type,
        int    minMobLevel,
        int    durationBase,
        double durationPerLevel,
        int    amplifierUpgradeEvery,
        double chanceBase,
        double chancePerLevel,
        double maxChance
) {

    /** Durée en ticks pour ce niveau de mob. 0 si le mob n'a pas encore atteint {@code minMobLevel}. */
    public int computeDuration(int mobLevel) {
        if (mobLevel < minMobLevel) return 0;
        return Math.max(1, (int) (durationBase + (mobLevel - minMobLevel) * durationPerLevel));
    }

    /** Probabilité (0.0–1.0) d'appliquer l'effet à cette attaque. */
    public double computeChance(int mobLevel) {
        if (mobLevel < minMobLevel) return 0;
        return Math.min(maxChance, chanceBase + (mobLevel - minMobLevel) * chancePerLevel);
    }

    /** Amplifier Bukkit (0 = niveau I, 1 = niveau II, etc.). */
    public int computeAmplifier(int mobLevel) {
        if (mobLevel < minMobLevel || amplifierUpgradeEvery <= 0) return 0;
        return (mobLevel - minMobLevel) / amplifierUpgradeEvery;
    }
}