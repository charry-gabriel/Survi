package fr.miuby.survi.quest;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Quête journalière individuelle.
 * Hérite des champs communs de {@link BaseQuest} (id, name, type, target,
 * goal, potionRewards) et ajoute uniquement ce qui lui est propre.
 */
@Getter
@SuperBuilder
public class Quest extends BaseQuest {
    private final EQuestDifficulty difficulty;
    private final int reputationReward;
}
