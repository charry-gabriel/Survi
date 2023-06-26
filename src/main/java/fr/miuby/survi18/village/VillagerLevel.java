package fr.miuby.survi18.village;

import fr.miuby.survi18.GameManager;
import fr.miuby.survi18.blessing.Blessing;
import fr.miuby.survi18.Tribute;
import org.bukkit.Location;
import org.bukkit.entity.Villager;

import java.util.Arrays;

public class VillagerLevel extends AVillager {
    private int level = 0;
    private final Tribute[] tributes;
    private final Blessing[] blessings;

    public VillagerLevel(Location location, String name, Villager.Type type, Villager.Profession profession, Tribute[] tributes, Blessing[] blessings) {
        super(location, name, type, profession);
        this.tributes = tributes;
        this.blessings = blessings;
    }

    public void SetLevel(int level) {
        this.level = level;
    }

    public void AddLevel() {
        this.level++;
        GameManager.getInstance().getDatabaseManager().updateVillager(name, level);
    }

    public Tribute getTribute() {
        return tributes[this.level];
    }

    public Blessing[] getCurrentBlessings() {
        return Arrays.copyOfRange(blessings, 0, this.level);
    }
}
