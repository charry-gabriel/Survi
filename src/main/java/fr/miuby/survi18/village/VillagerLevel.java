package fr.miuby.survi18.village;

import org.bukkit.Location;
import org.bukkit.entity.Villager;

public class VillagerLevel extends AVillager {
    private int level = 0;

    public VillagerLevel(Location location, String name, Villager.Type type, Villager.Profession profession) {
        super(location, name, type, profession);
    }
}
