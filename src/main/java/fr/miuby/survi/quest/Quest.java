package fr.miuby.survi.quest;

import lombok.Builder;
import lombok.Getter;
import org.bukkit.potion.PotionEffect;

import java.util.List;

@Getter
@Builder
public class Quest {
    private final String id;
    private final String name;
    private final String description;
    private final QuestType type;
    private final QuestDifficulty difficulty;
    
    // Target can be Material (MINE/CRAFT/COLLECT) or EntityType (KILL)
    private final Object target;
    private final int goal;
    
    // Rewards
    private final List<PotionEffect> rewards;
    private final int reputationReward;
}
