package fr.miuby.survi18.village;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

import java.util.LinkedHashMap;
import java.util.Objects;

public abstract class AVillager {
    protected Villager villager;
    protected Inventory inventory;
    protected LinkedHashMap<Material, ItemEtat> items;

    public AVillager(Location location, String name, Villager.Type type, Villager.Profession profession) {
        Villager v = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        v.customName(Component.text(name));
        v.setVillagerType(type);
        v.setProfession(profession);
        v.setAI(false);
        v.setCollidable(false);

        this.villager = v;
    }

    public void SetItems(LinkedHashMap<Material, ItemEtat> items) {
        this.items = items;

        //float size = items.size() / 9f;
        Inventory inv = Bukkit.createInventory(villager, InventoryType.CHEST, Objects.requireNonNull(villager.customName()));//(int) Math.ceil(size) * 9

        for (ItemEtat item : this.items.values()) {
            inv.addItem(item.getItem());
        }
        this.inventory = inv;
    }

    public Villager getVillager() {
        return villager;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public LinkedHashMap<Material, ItemEtat> getItems() {
        return items;
    }
}
