package fr.miuby.lib;

import fr.miuby.survi.GameManager;
import lombok.Getter;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

@Getter
public abstract class MLVillager {
    protected final String nameId;
    private final Villager.Type type;
    private final Villager.Profession profession;
    protected Villager villager;
    protected UUID uuid;
    protected TextComponent displayName;
    protected Inventory inventory;
    protected Location location;

    public static <T extends MLVillager> T create(Supplier<T> constructor, MLVillagerData data) {
        T villager = constructor.get();
        villager.init(data);
        return villager;
    }

    public MLVillager(String nameId, Villager.Type type, Villager.Profession profession) {
        this.nameId = nameId;
        this.type = type;
        this.profession = profession;
    }

    public final void init(@Nullable MLVillagerData villagerData) {
        if (villagerData == null) {
            setData(createDefaultData());
            createVillager();
            saveData();
        } else {
            setData(villagerData);
            findVillager(0);
        }
        onInitialized();
    }

    protected abstract MLVillagerData createDefaultData();
    protected abstract void saveData();
    protected abstract void onInitialized();

    protected void setData(MLVillagerData villagerData) {
        this.uuid = villagerData.getUuid();
        this.location = villagerData.getLocation();
    }

    private void createVillager() {
        this.villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        villager.setVillagerType(type);
        villager.setProfession(profession);
        villager.setAI(false);
        villager.setCollidable(false);
        villager.setSilent(true);

        this.uuid = this.villager.getUniqueId();
    }

    private void findVillager(int attempt) {
        if (attempt >= 40) {
            GameManager.getInstance().getLogger().warning("Unable to find villager " + nameId + " after waiting for chunk load.");
            createVillager();
        }

        if (location != null && !location.getChunk().isLoaded()) {
            location.getChunk().load();
        }

        assert location != null;
        Entity entity = location.getWorld().getEntity(uuid);
        if (entity != null && entity.getType() == EntityType.VILLAGER) {
            this.villager = (Villager)entity;
            GameManager.getInstance().getLogger().info("Villager " + nameId + " found after waiting " + attempt + " ticks.");
        } else {
            GameManager.getInstance().getScheduler().runTaskLater(GameManager.getInstance().getPlugin(), () -> findVillager(attempt + 1), 1L);
        }
    }
}