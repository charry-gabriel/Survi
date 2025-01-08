package fr.miuby.survi.villager;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.villager.blessing.Blessing;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.Monde;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AVillager {
    protected Villager villager;
    protected Inventory inventory;
    protected UUID uuid;
    protected final Blessing[] blessings;
    protected final TextComponent[] messages;
    protected int level = 0;
    protected String nameId;
    protected Location location;
    protected List<ItemStack> givenItems = new ArrayList<>();

    private final Villager.Type type;
    private final Villager.Profession profession;

    public AVillager(String name, Villager.Type type, Villager.Profession profession, Blessing[] blessings, TextComponent[] messages) {
        this.blessings = blessings;
        this.messages = messages;
        this.type = type;
        this.profession = profession;
        this.nameId = name;

        if (GameManager.getInstance().getDatabase().IsLoaded() && !GameManager.getInstance().getDatabase().initVillager(this, name)) {
            GameManager.getInstance().getLogger().warning(name + " doesn't exist");
            this.villager = CreateRealVillager(location, type, profession);
            this.uuid = this.villager.getUniqueId();
            GameManager.getInstance().getDatabase().CreateDBVillager(name, this.uuid);
        }
    }

    public static AVillager get(UUID uuid) {
        AVillager villager = GameManager.getInstance().getVillagerFactory().getVillagers().get(uuid);
        if (villager == null)
            throw new NullPointerException(uuid.toString() + " villager not found !");
        return villager;
    }

    public static boolean contains(UUID uuid) {
        return GameManager.getInstance().getVillagerFactory().getVillagers().containsKey(uuid);
    }

    private Villager CreateRealVillager(Location location, Villager.Type type, Villager.Profession profession) {
        GameManager.getInstance().getLogger().info("Creating Villager");
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        villager.setVillagerType(type);
        villager.setProfession(profession);
        villager.setAI(false);
        villager.setCollidable(false);
        return villager;
    }

    public abstract void giveItems(Inventory inventory, ItemStack item, Player player);

    public abstract void createInventory();

    public abstract TextComponent getName();

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

    public void setLevel(int level) {
        this.level = level;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setGivenItems(ItemStack[] givenItems) {
        this.givenItems = new ArrayList<>(List.of(givenItems));
    }
}