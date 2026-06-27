package fr.miuby.survi.item;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Holder d'inventaire pour le GUI du sac à dos.
 *
 * <p>{@code backpackId} identifie l'item physique (PDC {@code backpack_id}) afin de retrouver,
 * à la fermeture, l'exact ItemStack tenu par le joueur dans lequel sauvegarder le contenu.</p>
 */
@Getter
public class BackpackMenuHolder implements InventoryHolder {
    private final UUID backpackId;
    private final UUID ownerId;
    @Setter
    private Inventory inventory;

    public BackpackMenuHolder(UUID backpackId, UUID ownerId) {
        this.backpackId = backpackId;
        this.ownerId = ownerId;
    }

    @Override
    public @NonNull Inventory getInventory() {
        return inventory;
    }
}