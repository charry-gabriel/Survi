package fr.miuby.survi.job;

import fr.miuby.survi.system.SurviConfig;

import java.util.List;

/**
 * Configuration des seuils de réputation pour chaque niveau de métier.
 *
 * <p>Toutes les données (seuils, noms) sont lues depuis {@code config.yml}
 * via {@link SurviConfig} — plus rien n'est hardcodé ici.
 *
 * <p>Le niveau est commun à tous les métiers et se calcule uniquement
 * à partir de la réputation accumulée auprès du Trader associé au métier.
 */
public final class JobLevelConfig {

    private JobLevelConfig() {}

    // ─── Helpers internes ────────────────────────────────────────────────────────

    private static List<SurviConfig.JobLevelEntry> entries() {
        return SurviConfig.getInstance().getJobLevelEntries();
    }

    // ─── API publique ────────────────────────────────────────────────────────────

    /** Nombre de niveaux maximum (0 inclus), lu depuis la config. */
    public static int getMaxLevel() {
        return entries().size() - 1;
    }

    /**
     * Calcule le niveau de métier à partir d'une valeur de réputation.
     *
     * @param reputation réputation accumulée avec le Trader de ce métier
     * @return niveau entre 0 et {@link #getMaxLevel()}
     */
    public static int computeLevel(int reputation) {
        List<SurviConfig.JobLevelEntry> list = entries();
        int level = 0;
        for (int i = 1; i < list.size(); i++) {
            if (reputation >= list.get(i).threshold()) {
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
     * @param level niveau cible (0–{@link #getMaxLevel()})
     * @return seuil de réputation, ou -1 si le niveau est hors bornes
     */
    public static int getThreshold(int level) {
        List<SurviConfig.JobLevelEntry> list = entries();
        if (level < 0 || level >= list.size()) return -1;
        return list.get(level).threshold();
    }

    /**
     * Retourne la réputation nécessaire pour le prochain niveau,
     * ou -1 si le niveau max est déjà atteint.
     */
    public static int getNextThreshold(int currentReputation) {
        int level = computeLevel(currentReputation);
        if (level >= getMaxLevel()) return -1;
        return entries().get(level + 1).threshold();
    }
}
