package fr.miuby.survi18.village;

import fr.miuby.survi18.GameManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.metadata.FixedMetadataValue;

public abstract class AVillager {
    protected Villager villager;
    protected Inventory inventory;
    protected String name;

    public AVillager(Location location, String nameId, Villager.Type type, Villager.Profession profession) {
        Villager v = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        v.customName(Component.text(nameId));
        v.setVillagerType(type);
        v.setProfession(profession);
        v.setAI(false);
        v.setCollidable(false);
        v.setMetadata("name", new FixedMetadataValue(GameManager.getInstance().getPlugin(), nameId));

        this.villager = v;
        this.name = nameId;
    }

    public Villager getVillager() {
        return villager;
    }

    public Inventory getInventory() {
        return inventory;
    }
}
