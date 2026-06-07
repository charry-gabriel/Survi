package fr.miuby.survi.quest.quest;

import fr.miuby.survi.job.EJob;
import fr.miuby.survi.quest.BaseQuest;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Quête journalière individuelle.
 * Hérite des champs communs de {@link BaseQuest} (id, name, type, target,
 * goal, rewards) et ajoute uniquement ce qui lui est propre.
 */
@Getter
@SuperBuilder
public class Quest extends BaseQuest {

    /**
     * Niveau de difficulté de la quête (entier ≥ 1, croissant = plus difficile).
     * Remplace l'ancienne enum EQuestDifficulty (COMMON/RARE/LEGENDARY → 1/2/3).
     */
    private final int difficulty;

    /**
     * Métiers autorisés à recevoir cette quête.
     * Liste vide = tous les métiers peuvent la recevoir.
     * Permet de filtrer les quêtes selon le métier du joueur ou du Trader.
     */
    private final List<EJob> jobs;
}