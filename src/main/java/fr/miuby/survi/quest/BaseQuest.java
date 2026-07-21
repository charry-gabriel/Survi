package fr.miuby.survi.quest;

import fr.miuby.survi.blessing.Blessing;
import fr.miuby.survi.quest.globalquest.GlobalQuest;
import fr.miuby.survi.quest.quest.Quest;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

/**
 * Classe de base commune aux quêtes journalières ({@link Quest})
 * et aux quêtes globales ({@link GlobalQuest}).
 */
@Getter
@SuperBuilder
public abstract class BaseQuest {

    private final String id;
    private final String name;
    private final String description;
    private final EQuestType type;

    /**
     * Liste des cibles acceptées : Materials (MINE/CRAFT/SMELT/ENCHANT/HARVEST_BEEHIVE/ANVIL_ENCHANT),
     * EntityTypes (KILL/SHEAR/BREED/TAME) ou null (FISH, GAIN_XP_LEVELS).
     * Une seule entrée = comportement identique à l'ancienne cible unique.
     * Plusieurs entrées = toute cible de la liste est acceptée (ex: IRON_ORE + DEEPSLATE_IRON_ORE).
     */
    private final List<Object> targets;
    private final int goal;

    /**
     * Mode de combinaison de {@link #targets} : {@link ETargetsMode#ANY} (défaut, logique "OU",
     * compteur unique partagé) ou {@link ETargetsMode#ALL} (logique "ET", chaque cible doit
     * individuellement atteindre {@link #goal}). Toujours renseigné par {@link QuestYamlLoader}
     * (jamais null en pratique).
     */
    private final ETargetsMode targetsMode;

    /** Effets à appliquer en récompense. Jamais null (tableau vide si aucun effet). */
    private final Blessing rewards;

    /**
     * Retourne la description avec {@code {value}} remplacé par l'objectif numérique.
     * À utiliser partout où la description est affichée au joueur.
     */
    public String getFormattedDescription() {
        return description.replace("{value}", String.valueOf(goal));
    }

    /**
     * Vérifie si une action de jeu correspond à cette quête.
     * Centralise la logique type + cible partagée par QuestManager et GlobalQuestManager.
     *
     * <p>Types sans cible (FISH, GAIN_XP_LEVELS) : toujours vrai dès que le type correspond,
     * quelle que soit la valeur de {@code targets} dans le YAML.</p>
     */
    public boolean matchesAction(EQuestType actionType, Object actionTarget) {
        if (this.type != actionType) return false;
        return (actionType == EQuestType.FISH || actionType == EQuestType.GAIN_XP_LEVELS)
                || (this.targets == null || this.targets.isEmpty())
                || this.targets.contains(actionTarget);
    }

    /**
     * Clé canonique d'une cible (nom de l'enum Material/EntityType), utilisée pour indexer
     * la progression par cible en mode {@link ETargetsMode#ALL}.
     */
    public static String targetKey(Object target) {
        return (target instanceof Enum<?> enumTarget) ? enumTarget.name() : String.valueOf(target);
    }

    /**
     * Nom affichable d'une cible pour l'UI, ex : {@code HONEY_BOTTLE} -> {@code "Honey Bottle"}.
     */
    public static String formatTargetName(Object target) {
        String[] words = targetKey(target).split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (result.length() > 0) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase());
        }
        return result.toString();
    }

    /**
     * En mode {@link ETargetsMode#ALL}, vérifie que CHAQUE cible de {@link #targets} atteint
     * {@link #goal} dans {@code progressByTarget}. Retourne toujours false si {@link #targets}
     * est vide/null (mode ALL non applicable sans au moins une cible).
     */
    public boolean isTargetProgressComplete(Map<String, Integer> progressByTarget) {
        if (targets == null || targets.isEmpty()) return false;
        for (Object target : targets) {
            if (progressByTarget.getOrDefault(targetKey(target), 0) < goal) return false;
        }
        return true;
    }

    /**
     * Construit le texte de progression détaillé par cible pour le mode {@link ETargetsMode#ALL},
     * ex : {@code "Honeycomb 12/12 · Honey Bottle 5/12"}. Chaque cible est plafonnée à
     * {@link #goal} pour l'affichage. Chaîne vide si {@link #targets} est vide/null.
     */
    public String formatTargetProgressBreakdown(Map<String, Integer> progressByTarget) {
        if (targets == null || targets.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        for (Object target : targets) {
            if (result.length() > 0) result.append(" · ");
            int current = Math.min(goal, progressByTarget.getOrDefault(targetKey(target), 0));
            result.append(formatTargetName(target)).append(' ').append(current).append('/').append(goal);
        }
        return result.toString();
    }
}