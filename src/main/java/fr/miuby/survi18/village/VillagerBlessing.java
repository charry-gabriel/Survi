package fr.miuby.survi18.village;

import fr.miuby.survi18.blessing.Blessing;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Villager;

public abstract class VillagerBlessing extends AVillager {
    protected final Blessing[] blessings;
    protected final Component[] messages;

    public VillagerBlessing(Location location, String name, Villager.Type type, Villager.Profession profession, Blessing[] blessings, Component[] messages) {
        super(location, name, type, profession);
        this.blessings = blessings;
        this.messages = messages;
    }
}
