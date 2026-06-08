package fr.miuby.survi.quest;

import fr.miuby.survi.blessing.Blessing;
import fr.miuby.survi.quest.globalquest.GlobalQuest;
import fr.miuby.survi.quest.quest.Quest;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

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
     * Liste des cibles acceptées : Materials (MINE/CRAFT/SMELT), EntityTypes (KILL/SHEAR/BREED) ou null (FISH).
     * Une seule entrée = comportement identique à l'ancienne cible unique.
     * Plusieurs entrées = toute cible de la liste est acceptée (ex: IRON_ORE + DEEPSLATE_IRON_ORE).
     */
    private final List<Object> targets;
    private final int goal;

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
     */
    public boolean matchesAction(EQuestType actionType, Object actionTarget) {
        if (this.type != actionType) return false;
        return (actionType == EQuestType.FISH)
                || (this.targets == null || this.targets.isEmpty())
                || this.targets.contains(actionTarget);
    }
}