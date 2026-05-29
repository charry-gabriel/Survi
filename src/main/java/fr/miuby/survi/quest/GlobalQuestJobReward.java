package fr.miuby.survi.quest;

import fr.miuby.survi.job.EJob;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GlobalQuestJobReward {
    private final EJob job;
    private final int reputation;
}
