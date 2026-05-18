package fr.miuby.survi.villager.trader;

import java.util.List;

public class TraderConfig {
    public String nameId;
    public String displayName;
    public String type;
    public String profession;
    public String job;               // optionnel — nom de l'enum EJob
    public String questDifficulty;   // optionnel — nom de l'enum QuestDifficulty
    public String openMessage;
    public String mainHandItem;      // optionnel — material vanilla à équiper en main
    public List<TraderRecipeConfig> recipes;
}