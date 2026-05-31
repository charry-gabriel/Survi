package fr.miuby.survi.quest;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Quête globale lancée manuellement par un admin.
 * Hérite des champs communs de {@link BaseQuest} (id, name, type, target,
 * goal, rewards) et ajoute uniquement ce qui lui est propre.
 */
@Getter
@SuperBuilder
public class GlobalQuest extends BaseQuest {

    /** Durée maximale pour compléter la quête (en secondes). */
    private final int timeLimitSeconds;
}
