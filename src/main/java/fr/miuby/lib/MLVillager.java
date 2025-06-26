package fr.miuby.lib;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;

import javax.annotation.Nullable;
import java.util.function.Supplier;

@Getter
public abstract class MLVillager {
    protected final String nameId;
    private final Villager.Type type;
    private final Villager.Profession profession;
    protected Villager villager;
    protected TextComponent displayName;
    protected Inventory inventory;

    @Setter(AccessLevel.PROTECTED)
    @Getter(AccessLevel.NONE)
    private MLVillagerData villagerData;

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

    protected void destroy() {
        if (villager != null) {
            villager.remove();
        }
    }

    protected abstract MLVillagerData createDefaultData();
    protected void saveData() { }

    protected void onInitialized() {
        getVillager().customName(getDisplayName());
        createInventory();
    }

    protected void setData(MLVillagerData villagerData) {
        this.villagerData = villagerData;
    }

    protected void createInventory() {
        this.inventory = this.getVillager().getInventory();
    }

    private void createVillager() {
        this.villager = (Villager) villagerData.location.getWorld().spawnEntity(villagerData.location, EntityType.VILLAGER);
        villager.setVillagerType(type);
        villager.setProfession(profession);
        villager.setAI(false);
        villager.setCollidable(false);
        villager.setSilent(true);
    }

    private void findVillager(int attempt) {
        if (attempt >= 40) {
            MiubyLib.getLogger().warning("Unable to find villager " + nameId + " after waiting for chunk load.");
            createVillager();
        }

        if (!villagerData.location.getChunk().isLoaded()) {
            villagerData.location.getChunk().load();
        }

        Entity entity = villagerData.location.getWorld().getEntity(villagerData.uuid);
        if (entity != null && entity.getType() == EntityType.VILLAGER) {
            this.villager = (Villager)entity;
            MiubyLib.getLogger().info("Villager " + nameId + " found after waiting " + attempt + " ticks.");
        } else {
            MiubyLib.runLater(() -> findVillager(attempt + 1), 1L);
        }
    }
}