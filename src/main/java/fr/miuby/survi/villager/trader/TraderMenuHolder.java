package fr.miuby.survi.villager.trader;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jspecify.annotations.NonNull;

@Getter
public class TraderMenuHolder implements InventoryHolder {
    private final Trader trader;
    @Setter
    private Inventory inventory;

    public TraderMenuHolder(Trader trader) {
        this.trader = trader;
    }

    @Override
    public @NonNull Inventory getInventory() {
        return inventory;
    }
}