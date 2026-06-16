package fr.miuby.survi.villager.villagerlevel;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Holder de l'inventaire tribut d'un {@link VillagerLevel}.
 *
 * <p>Remplace l'ancien usage du {@code Villager} Bukkit comme holder,
 * ce qui n'est plus possible depuis le passage des PNJ niveau au type {@code Mannequin}
 * (qui n'implémente pas {@code InventoryHolder}).</p>
 *
 * <p>Utilisé dans {@code ItemListener} pour identifier les inventaires tributs
 * lors des {@code InventoryClickEvent}.</p>
 */
public final class VillagerTributeHolder implements InventoryHolder {
    private final VillagerLevel villagerLevel;

    public VillagerTributeHolder(VillagerLevel villagerLevel) {
        this.villagerLevel = villagerLevel;
    }

    public VillagerLevel getVillagerLevel() {
        return villagerLevel;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return villagerLevel.getInventory();
    }
}
