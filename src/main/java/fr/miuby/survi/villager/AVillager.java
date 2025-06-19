package fr.miuby.survi.villager;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.world.WorldFactory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;

import javax.annotation.Nullable;
import java.util.UUID;

@RequiredArgsConstructor
public abstract class AVillager {
    protected final String nameId;
    private final Villager.Type type;
    private final Villager.Profession profession;
    protected final TextComponent[] messages;
    @Getter
    protected final TextComponent openMessage;

    @Getter
    protected Villager villager;
    @Getter
    protected Inventory inventory;
    @Setter
    protected Location location;
    protected UUID uuid;

    public void initVillager() {
        if (GameManager.getInstance().getDatabase().IsLoaded() && !GameManager.getInstance().getDatabase().initVillager(this, this.nameId)) {
            GameManager.getInstance().getLogger().warning(this.nameId + " doesn't exist");
            this.villager = CreateRealVillager(new Location(WorldFactory.getDefaultWorld(), 0, 700, 0), type, profession);
            this.uuid = this.villager.getUniqueId();
            GameManager.getInstance().getDatabase().CreateDBVillager(this.nameId, this.uuid);
        }
    }

    @Nullable
    public static AVillager get(UUID uuid) {
        if (GameManager.getInstance().getVillagerFactory().getVillagers().containsKey(uuid))
            return GameManager.getInstance().getVillagerFactory().getVillagers().get(uuid);

        return null;
    }

    private Villager CreateRealVillager(Location location, Villager.Type type, Villager.Profession profession) {
        GameManager.getInstance().getLogger().info("Creating Villager");
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        villager.setVillagerType(type);
        villager.setProfession(profession);
        villager.setAI(false);
        villager.setCollidable(false);
        villager.setSilent(true);
        return villager;
    }

    public abstract void createInventory();

    public abstract TextComponent getDisplayName();

    public void setRealVillager(UUID uuid) {
        Villager realVillager = findRealVillager(uuid);

        if (realVillager != null) {
            this.villager = realVillager;
            this.uuid = uuid;
        } else {
            GameManager.getInstance().getLogger().info("Didn't find villager uuid");

            if (GameManager.getInstance().getDatabase().IsLoaded()) {
                this.villager = CreateRealVillager(location, type, profession);
                this.uuid = this.villager.getUniqueId();
                GameManager.getInstance().getDatabase().updateVillagerUUID(this.uuid, nameId);
            }
        }
    }

    @Nullable
    private Villager findRealVillager(UUID uuid) {
        Entity entity = WorldFactory.getDefaultWorld().getEntity(uuid);

        if (entity != null && entity.getType() == EntityType.VILLAGER) {
            return (Villager)entity;
        }
        return null;
    }

}