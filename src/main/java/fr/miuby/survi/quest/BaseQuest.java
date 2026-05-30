package fr.miuby.survi.quest;

import fr.miuby.survi.blessing.Blessing;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

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

    /** Material (MINE/CRAFT/SMELT), EntityType (KILL/SHEAR/BREED) ou null (FISH). */
    private final Object target;
    private final int goal;

    /** Effets à appliquer en récompense. Jamais null (tableau vide si aucun effet). */
    private final Blessing rewards;

    /**
     * Vérifie si une action de jeu correspond à cette quête.
     * Centralise la logique type + cible partagée par QuestManager et GlobalQuestManager.
     */
    public boolean matchesAction(EQuestType actionType, Object actionTarget) {
        if (this.type != actionType) return false;
        return (actionType == EQuestType.FISH)
                || (this.target == null)
                || this.target.equals(actionTarget);
    }
}
