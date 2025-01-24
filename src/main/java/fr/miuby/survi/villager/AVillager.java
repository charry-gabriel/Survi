package fr.miuby.survi.villager;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.Monde;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;

import javax.annotation.Nullable;
import java.util.UUID;

public abstract class AVillager {
    protected Villager villager;
    protected Inventory inventory;
    protected UUID uuid;
    protected final TextComponent[] messages;
    protected final TextComponent openMessage;
    protected String nameId;
    protected Location location;

    private final Villager.Type type;
    private final Villager.Profession profession;

    public AVillager(String nameId, Villager.Type type, Villager.Profession profession, TextComponent[] messages, TextComponent openMessage) {
        this.messages = messages;
        this.type = type;
        this.profession = profession;
        this.nameId = nameId;
        this.openMessage = openMessage;
    }

    public void initVillager() {
        if (GameManager.getInstance().getDatabase().IsLoaded() && !GameManager.getInstance().getDatabase().initVillager(this, this.nameId)) {
            GameManager.getInstance().getLogger().warning(this.nameId + " doesn't exist");
            this.villager = CreateRealVillager(new Location(GameManager.getInstance().getWorldFactory().getWorld(EWorld.VILLAGE).getWorld(), 0, 700, 0), type, profession);
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

    public Villager getVillager() {
        return villager;
    }

    public Inventory getInventory() {
        return inventory;
    }

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
        Entity entity = Monde.get(EWorld.VILLAGE).getWorld().getEntity(uuid);

        if (entity != null && entity.getType() == EntityType.VILLAGER) {
            return (Villager)entity;
        }
        return null;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public TextComponent getOpenMessage() {
        return openMessage;
    }
}