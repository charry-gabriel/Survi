package fr.miuby.survi.quest;

import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;

@Getter
public enum QuestDifficulty {
    COMMON(NamedTextColor.GRAY, 1.0f, 10),
    RARE(NamedTextColor.BLUE, 2.0f, 25),
    LEGENDARY(NamedTextColor.GOLD, 5.0f, 100);

    private final NamedTextColor color;
    private final float rewardMultiplier;
    private final int reputationGain;

    QuestDifficulty(NamedTextColor color, float rewardMultiplier, int reputationGain) {
        this.color = color;
        this.rewardMultiplier = rewardMultiplier;
        this.reputationGain = reputationGain;
    }
}
