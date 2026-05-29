package fr.miuby.survi.villager.villagerlevel;

import fr.miuby.survi.item.SimpleItemStack;

import java.util.List;
import java.util.Map;

public class VillagerLevelConfig {
    public String name;
    public String message = "";
    public String recap = "";
    public List<SimpleItemStack> tribute;
    public List<Map<String, Object>> blessings;
    /** Durée de lock en jours après avoir complété ce niveau. Null = pas de lock. */
    public Integer lock;
}