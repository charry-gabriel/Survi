package fr.miuby.survi.mob;

/**
 * Configuration de la durée de mèche (fuse-ticks) du Creeper.
 *
 * <p>Formule : {@code ticks = max(min, base + (level - 1) * perLevel)}
 * <br>Exemple avec base=30, perLevel=-0.2, min=5 :
 * <ul>
 *   <li>Niveau  1 → 30 ticks (0,25 s × 6 = 1,5 s) — vanilla</li>
 *   <li>Niveau 50 → 20,2 ticks (≈ 1 s)</li>
 *   <li>Niveau 100 → 10,2 ticks (≈ 0,5 s) — très rapide</li>
 *   <li>Niveau 200 → 5 ticks (min, ≈ 0,25 s) — quasi-instantané</li>
 * </ul>
 */
public record MobFuseConfig(boolean enabled, double base, double perLevel, int min) {

    /**
     * @return la durée de mèche calculée en ticks, bornée par {@code min}.
     *         Retourne {@code -1} si désactivée.
     */
    public double compute(int level) {
        if (!enabled) return -1;
        return Math.max(min, base + (level - 1) * perLevel);
    }
}