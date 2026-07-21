package fr.miuby.survi.quest.quest;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
@AllArgsConstructor
public class PlayerQuestData {
    private int slot;
    private String questId;
    private int progress;
    private LocalDate lastAccepted;
    private boolean isCompleted;
    private String traderId;
    private boolean claimed;

    /**
     * Progression par cible, utilisée uniquement quand la quête est en mode
     * {@link fr.miuby.survi.quest.ETargetsMode#ALL} (clé = {@code BaseQuest.targetKey(target)}).
     * Toujours non-null (map vide si mode ANY ou si le mode ALL n'a pas encore progressé).
     */
    private Map<String, Integer> targetProgress;
}