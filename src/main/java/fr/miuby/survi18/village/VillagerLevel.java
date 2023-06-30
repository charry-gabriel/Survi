package fr.miuby.survi18.village;

import fr.miuby.survi18.AlphaPlayer;
import fr.miuby.survi18.GameManager;
import fr.miuby.survi18.blessing.Blessing;
import fr.miuby.survi18.Tribute;
import fr.miuby.survi18.blessing.BlessingEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;

public class VillagerLevel extends AVillager {
    private int level = 0;
    private final Tribute[] tributes;
    private final Blessing[] blessings;
    private final Component[] messages;
    private final Component[] names;

    public VillagerLevel(Location location, String name, Villager.Type type, Villager.Profession profession, Tribute[] tributes, Blessing[] blessings, Component[] messages, Component[] names) {
        super(location, name, type, profession);
        this.tributes = tributes;
        this.blessings = blessings;
        this.messages = messages;
        this.names = names;

        updateInventory();
    }

    public void GiveItems(Inventory inventory, ItemStack item, Player player){
        removeItemStack(inventory, item, player);

        if (inventory.isEmpty()) {
            Bukkit.broadcast(getMessage().color(NamedTextColor.AQUA));
            applyBlessing();
            addLevel();
            villager.customName(getName());
            updateInventory();
            player.closeInventory();
            player.openInventory(villager.getInventory());
        }
    }

    /*public void CreateDBVillager(Connection connection) {
        final PreparedStatement preparedStatement;
        try {
            preparedStatement = connection.prepareStatement("INSERT INTO villager VALUES (?, ?)");
            preparedStatement.setString(1, name);
            preparedStatement.setInt(2, level);
            preparedStatement.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }*/

    public void SetLevel(int level) {
        this.level = level;
        villager.customName(getName());
        updateInventory();
    }

    public void updateInventory() {
        //float size = items.size() / 9f;
        Inventory inv = Bukkit.createInventory(villager, InventoryType.CHEST, Objects.requireNonNull(villager.customName()));//(int) Math.ceil(size) * 9

        for (ItemStack item : getTribute().getItemStacks())
            inv.setItem(inv.firstEmpty(), item);

        this.inventory = inv;
    }

    public void addLevel() {
        this.level++;
        GameManager.getInstance().getDatabaseManager().updateVillager(name, level);
    }

    public Tribute getTribute() {
        return tributes[this.level];
    }

    public Blessing getBlessing() {
        return blessings[this.level];
    }

    public Component getMessage() {
        return messages[this.level];
    }

    public Component getName() {
        return names[this.level];
    }

    public Blessing[] getCurrentBlessings() {
        return Arrays.copyOfRange(blessings, 0, this.level);
    }

    public void ApplyAllCurrentBlessing(AlphaPlayer player) {
        for (Blessing blessing : getCurrentBlessings()) {
            for (BlessingEffect effect : blessing.getBlessingEffects()) {
                effect.applyEffect(player);
            }
        }
    }

    public void applyBlessing() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (BlessingEffect effect : getBlessing().getBlessingEffects()) {
                effect.applyEffect(GameManager.getInstance().getAlphaPlayer(player.getUniqueId()));
            }
        }
    }

    public void removeItemStack(Inventory inventory, ItemStack item, Player player) {
        ItemStack itemToRemove = null;

        for (ItemStack tributeItem : inventory.getContents()) {
            if (tributeItem != null && tributeItem.getType() == item.getType()) {
                GameManager.getInstance().getLogger().info(name + " recupere " + item.getAmount() + " de " + item.getType().name());
                if (item.getAmount() < tributeItem.getAmount()) {
                    tributeItem.setAmount(tributeItem.getAmount() - item.getAmount());
                    player.getInventory().remove(item);
                } else {
                    itemToRemove = tributeItem;
                    item.setAmount(item.getAmount() - tributeItem.getAmount());
                }
                break;
            }
        }

        if (itemToRemove != null)
            inventory.removeItem(itemToRemove);
    }
}
