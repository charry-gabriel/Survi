package fr.miuby.survi.quest;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Quête globale lancée manuellement par un admin.
 * Hérite des champs communs de {@link BaseQuest} (id, name, type, target,
 * goal, potionRewards) et ajoute uniquement ce qui lui est propre.
 */
@Getter
@SuperBuilder
public class GlobalQuest extends BaseQuest {

    /** Durée maximale pour compléter la quête (en secondes). */
    private final int timeLimitSeconds;

    /** Réputation accordée dans chaque métier configuré à tous les participants. */
    private final List<GlobalQuestJobReward> jobRewards;
}
