package fr.miuby.survi.quest;

import fr.miuby.survi.job.EJob;

import java.time.LocalDate;

/**
 * Entrée d'historique pour une quête complétée (journalière ou globale).
 * Persistée dans la table {@code quest_history}.
 *
 * @param difficulty   Difficulté de la quête ({@link Quest#getDifficulty()}) ou niveau du monde pour les globales.
 * @param job          Nom du métier ({@link EJob#name()}) ou {@code null} si aucun métier spécifique.
 * @param questType    {@code "daily"} ou {@code "global"}.
 * @param contribution Contribution individuelle au progrès (0 pour les journalières, valeur réelle pour les globales).
 */
public record QuestHistoryEntry(int id, String playerUuid, String playerPseudo, String questId, LocalDate completedAt, int difficulty, String job, String questType, int contribution) {}
