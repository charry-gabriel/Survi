package fr.miuby.survi.villager;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.villager.blessing.Blessing;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public abstract class AVillager {
    protected Villager villager;
    protected Inventory inventory;
    protected UUID uuid;
    protected final Blessing[] blessings;
    protected final Component[] messages;
    protected int level = 0;

    public AVillager(String name, Location location, Villager.Type type, Villager.Profession profession, Blessing[] blessings, Component[] messages) {
        this.blessings = blessings;
        this.messages = messages;

        if (!GameManager.getInstance().getDatabase().getVillager(this, name)) {
            this.villager = CreateWorldVillager(location, type, profession);
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

    private Villager CreateWorldVillager(Location location, Villager.Type type, Villager.Profession profession) {
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        villager.setVillagerType(type);
        villager.setProfession(profession);
        villager.setAI(false);
        villager.setCollidable(false);
        return villager;
    }

    public abstract void giveItems(Inventory inventory, ItemStack item, Player player);

    public abstract void updateInventory();

    public abstract Component getName();

    public Villager getVillager() {
        return villager;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public boolean setUUID(UUID uuid) {
        Entity entity = Bukkit.getEntity(uuid);
        if (!(entity instanceof Villager))
            return false;

        this.uuid = uuid;
        this.villager = (Villager) entity;
        return true;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}