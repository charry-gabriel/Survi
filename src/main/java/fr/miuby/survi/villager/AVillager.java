package fr.miuby.survi.villager;

import fr.miuby.lib.MLVillager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.world.WorldFactory;
import lombok.Getter;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;

import java.util.function.Supplier;

public abstract class AVillager extends MLVillager {
    protected final TextComponent[] messages;
    @Getter
    protected final TextComponent openMessage;

    @Getter
    protected Inventory inventory;

    public static <T extends MLVillager> T create(Supplier<T> constructor) {
        T villager = constructor.get();
        villager.init(GameManager.getInstance().getDatabase().initVillager(villager.getNameId()));
        return villager;
    }

    public AVillager(String nameId, Villager.Type type, Villager.Profession profession, TextComponent[] messages, TextComponent openMessage) {
        super(nameId, type, profession);

        this.messages = messages;
        this.openMessage = openMessage;
    }

    @Override
    protected void saveData() {
        GameManager.getInstance().getDatabase().CreateDBVillager(this.nameId, this.uuid);
    }

    @Override
    protected AlphaVillagerData createDefaultData() {
        return new AlphaVillagerData(null, new Location(WorldFactory.getDefaultWorld(), 0, 700, 0));
    }

    public abstract void createInventory();

    public abstract TextComponent getDisplayName();
}