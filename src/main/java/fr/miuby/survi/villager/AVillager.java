package fr.miuby.survi.villager;

import fr.miuby.lib.MLVillager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.world.WorldFactory;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Villager;

import java.util.function.Supplier;

public abstract class AVillager extends MLVillager {
    protected final TextComponent[] messages;

    public static <T extends MLVillager> T create(Supplier<T> constructor) {
        T villager = constructor.get();
        villager.init(GameManager.getInstance().getDatabase().initVillager(villager.getNameId()));
        return villager;
    }

    public AVillager(String nameId, Villager.Type type, Villager.Profession profession, TextComponent[] messages) {
        super(nameId, type, profession);

        this.messages = messages;
    }

    @Override
    protected void saveData() {
        GameManager.getInstance().getDatabase().CreateDBVillager(this.nameId, this.getVillager().getUniqueId());
    }

    @Override
    protected AlphaVillagerData createDefaultData() {
        return new AlphaVillagerData(null, new Location(WorldFactory.getDefaultWorld(), 0, 700, 0));
    }
}