package fr.miuby.survi.job;

/**
 * Configuration des seuils de réputation pour chaque niveau de métier.
 *
 * Le niveau est commun à tous les métiers et se calcule uniquement
 * à partir de la réputation accumulée auprès du Trader associé au métier.
 *
 * Niveaux disponibles : 0 à 5.
 */
public final class JobLevelConfig {

    private JobLevelConfig() {}

    /**
     * Seuils de réputation nécessaires pour atteindre chaque niveau.
     * L'index correspond au niveau (0 = départ, toujours acquis).
     */
    private static final int[] LEVEL_THRESHOLDS = {
            0,   // Niveau 0 — Inconnu    (tout le monde démarre ici)
            10,  // Niveau 1 — Apprenti
            30,  // Niveau 2 — Journalier
            60,  // Niveau 3 — Expert
            100, // Niveau 4 — Maître
            200  // Niveau 5 — Légendaire
    };

    /** Nombre de niveaux maximum (0 inclus). */
    public static final int MAX_LEVEL = LEVEL_THRESHOLDS.length - 1;

    /**
     * Calcule le niveau de métier à partir d'une valeur de réputation.
     *
     * @param reputation réputation accumulée avec le Trader de ce métier
     * @return niveau entre 0 et {@link #MAX_LEVEL}
     */
    public static int computeLevel(int reputation) {
        int level = 0;
        for (int i = 1; i < LEVEL_THRESHOLDS.length; i++) {
            if (reputation >= LEVEL_THRESHOLDS[i]) {
                level = i;
            } else {
                break;
            }
        }
        return level;
    }

    /**
     * Retourne la réputation nécessaire pour atteindre le niveau donné.
     *
     * @param level niveau cible (0–{@link #MAX_LEVEL})
     * @return seuil de réputation, ou -1 si le niveau est hors bornes
     */
    public static int getThreshold(int level) {
        if (level < 0 || level >= LEVEL_THRESHOLDS.length) return -1;
        return LEVEL_THRESHOLDS[level];
    }

    /**
     * Retourne la réputation nécessaire pour le prochain niveau,
     * ou -1 si le niveau max est déjà atteint.
     */
    public static int getNextThreshold(int currentReputation) {
        int level = computeLevel(currentReputation);
        if (level >= MAX_LEVEL) return -1;
        return LEVEL_THRESHOLDS[level + 1];
    }

    /**
     * Nom textuel du niveau pour l'affichage.
     */
    public static String getLevelName(int level) {
        return switch (level) {
            case 0 -> "Inconnu";
            case 1 -> "Apprenti";
            case 2 -> "Journalier";
            case 3 -> "Expert";
            case 4 -> "Maître";
            case 5 -> "Légendaire";
            default -> "Niveau " + level;
        };
    }
}