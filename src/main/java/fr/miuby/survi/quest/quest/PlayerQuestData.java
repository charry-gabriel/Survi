package fr.miuby.survi.quest.quest;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

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
}