package fr.miuby.survi.quest;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class PlayerQuestData {
    private String questId;
    private int progress;
    private LocalDate lastAccepted;
    private boolean isCompleted;
    private String traderId;
    private boolean claimed;
}
