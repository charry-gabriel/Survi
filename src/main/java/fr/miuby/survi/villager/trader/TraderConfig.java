package fr.miuby.survi.villager.trader;

import java.util.List;

public class TraderConfig {
    public String nameId;
    public String displayName;
    public String type;
    public String profession;
    public String job;               // optionnel — nom de l'enum EJob
    public int questDifficulty;      // optionnel — niveau de difficulté (int ≥ 1, 0 = aléatoire)
    public String openMessage;
    public String mainHandItem;      // optionnel — material vanilla à équiper en main
    public List<TraderRecipeConfig> recipes;
}