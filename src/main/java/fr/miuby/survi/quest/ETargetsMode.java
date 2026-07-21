package fr.miuby.survi.quest;

/**
 * Mode de combinaison des cibles ({@code targets}) d'une quête.
 */
public enum ETargetsMode {

    /**
     * Une seule cible parmi {@code targets} suffit (défaut). La progression est cumulée
     * dans un compteur unique partagé entre toutes les cibles acceptées : 12 honeycomb,
     * 12 honey_bottle, ou 6 des deux valident une quête avec goal=12 (logique "OU").
     */
    ANY,

    /**
     * Chaque cible de {@code targets} doit individuellement atteindre {@code goal}.
     * La progression est suivie séparément par cible et la quête n'est terminée que
     * lorsque toutes les cibles ont atteint l'objectif (logique "ET").
     */
    ALL
}
