package fr.miuby.survi.mob;

/**
 * Configuration d'une stat pour un type de mob.
 * Formule linéaire : {@code valeur = base + (level - 1) * perLevel}
 *
 * <ul>
 *   <li>Niveau  1 → {@code base} (identique au vanilla si calibré ainsi)</li>
 *   <li>Niveau 10 → {@code base + 9  * perLevel}</li>
 *   <li>Niveau 100→ {@code base + 99 * perLevel}</li>
 * </ul>
 */
public record MobStatConfig(boolean enabled, double base, double perLevel) {

    /**
     * @return la valeur calculée pour ce niveau, ou {@code -1} si la stat est désactivée.
     */
    public double compute(int level) {
        if (!enabled) return -1;
        return base + (level - 1) * perLevel;
    }
}