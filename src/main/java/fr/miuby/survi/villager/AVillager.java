package fr.miuby.survi.villager;

import fr.miuby.lib.villager.MLVillager;
import fr.miuby.lib.villager.MLVillagerData;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.world.WorldInitializer;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Villager;

import org.jetbrains.annotations.Nullable;

public abstract class AVillager extends MLVillager {
    protected final TextComponent[] messages;

    public AVillager(String nameId, Villager.Type type, Villager.Profession profession, TextComponent[] messages) {
        super(nameId, type, profession);

        this.messages = messages;
    }

    @Override
    protected @Nullable MLVillagerData loadData() {
        return GameManager.getInstance().getDatabase().villagers().load(this.getNameId());
    }

    @Override
    protected void saveData() {
        GameManager.getInstance().getDatabase().villagers().create(this.nameId, this.getVillager().getUniqueId());
    }

    @Override
    protected AlphaVillagerData createDefaultData() {
        return new AlphaVillagerData(null, nameId, new Location(WorldInitializer.getDefaultWorld(), -23.5, 184.5, -19.5));
    }
}