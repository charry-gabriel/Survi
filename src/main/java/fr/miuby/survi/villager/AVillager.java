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
        this.uuid = uuid;
        Villager realVillager = findRealVillager(uuid);

        if (realVillager != null) {
            this.villager = realVillager;
            return;
        }

        // Villager not yet found – most likely because the chunk is not loaded.
        GameManager.getInstance().getLogger().info("Villager " + nameId + " not yet loaded, waiting for chunk...");
        waitForVillager(0);
    }

    /**
     * Recursively attempts to find the real villager once the chunk is loaded.
     * Tries up to 40 times (≈2 s). Stops early once the villager is found.
     */
    private void waitForVillager(int attempt) {
        if (attempt >= 40) {
            GameManager.getInstance().getLogger().warning("Unable to find villager " + nameId + " after waiting for chunk load.");
            return;
        }

        // Ensure the chunk containing the expected location is loaded
        if (location != null && !location.getChunk().isLoaded()) {
            location.getChunk().load();
        }

        Villager realVillager = findRealVillager(uuid);
        if (realVillager != null) {
            this.villager = realVillager;
            GameManager.getInstance().getLogger().info("Villager " + nameId + " found after waiting " + attempt + " ticks.");
        } else {
            // Try again next tick
            GameManager.getInstance().getScheduler().runTaskLater(GameManager.getInstance().getPlugin(), () -> waitForVillager(attempt + 1), 1L);
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