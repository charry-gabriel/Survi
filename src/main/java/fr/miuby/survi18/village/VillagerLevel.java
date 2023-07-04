package fr.miuby.survi18.village;

import fr.miuby.survi18.AlphaPlayer;
import fr.miuby.survi18.GameManager;
import fr.miuby.survi18.blessing.Blessing;
import fr.miuby.survi18.Tribute;
import fr.miuby.survi18.blessing.BlessingEffect;
import fr.miuby.survi18.database.DbConnection;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Objects;

public class VillagerLevel extends VillagerBlessing {
    private int level = 0;
    private final Tribute[] tributes;
    private final Component[] names;

    public VillagerLevel(Location location, String name, Villager.Type type, Villager.Profession profession, Blessing[] blessings, Component[] messages, Tribute[] tributes, Component[] names) {
        super(location, name, type, profession, blessings, messages);
        this.tributes = tributes;
        this.names = names;
        villager.setMetadata("level", new FixedMetadataValue(GameManager.getInstance().getPlugin(), 0));

        Bukkit.getScheduler().runTaskAsynchronously(GameManager.getInstance().getPlugin(), () -> {
            final DbConnection dbConnection = GameManager.getInstance().getDatabaseManager().getDbConnection();
            try {
                final Connection connection = dbConnection.getConnection();
                final PreparedStatement preparedStatement = connection.prepareStatement("SELECT name, level FROM villager WHERE name = '"+name+"'");
                final ResultSet resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    level = resultSet.getInt("level");
                }else{
                    CreateDBVillager(connection);
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });

        updateInventory();
    }

    public void GiveItems(Inventory inventory, ItemStack item, Player player){
        removeItemStack(inventory, item, player);

        if (inventory.isEmpty()) {
            Bukkit.broadcast(Component.text("<").color(NamedTextColor.AQUA).append(getName()).append(Component.text("> ").color(NamedTextColor.AQUA)).append(getMessage()));
            applyBlessing();
            addLevel();
            villager.customName(getName());
            updateInventory();
            player.closeInventory();
        }
    }

    public void CreateDBVillager(Connection connection) {
        final PreparedStatement preparedStatement;
        try {
            preparedStatement = connection.prepareStatement("INSERT INTO villager VALUES (?, ?)");
            preparedStatement.setString(1, name);
            preparedStatement.setInt(2, level);
            preparedStatement.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void SetLevel(int level) {
        this.level = level;
        villager.customName(getName());
        updateInventory();
    }

    public void updateInventory() {
        Inventory inv = Bukkit.createInventory(villager, InventoryType.CHEST, Objects.requireNonNull(villager.customName()));

        for (ItemStack item : getTribute().getItemStacks())
            inv.addItem(item);

        this.inventory = inv;
    }

    public void addLevel() {
        this.level++;
        GameManager.getInstance().getDatabaseManager().updateVillager(name, level);
    }

    public void ApplyAllCurrentBlessing(AlphaPlayer player) {
        for (Blessing blessing : getCurrentBlessings()) {
            for (BlessingEffect effect : blessing.getBlessingEffects()) {
                effect.applyEffect(player);
            }
        }
    }

    public void applyBlessing() {
        Sound myCustomSound = Sound.sound(Key.key("ui.toast.challenge_complete"), Sound.Source.AMBIENT, 1f, 1.1f);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(myCustomSound);

            for (BlessingEffect effect : getBlessing().getBlessingEffects()) {
                AlphaPlayer alphaPlayer = GameManager.getInstance().getAlphaPlayers().get(player.getUniqueId());
                effect.applyEffect(alphaPlayer);
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



    public Tribute getTribute() {
        return tributes[this.level];
    }

    public Blessing getBlessing() {
        return blessings[this.level];
    }

    public Component getMessage() {
        return messages[this.level].color(NamedTextColor.AQUA);
    }

    public Component getName() {
        return names[this.level].color(NamedTextColor.AQUA);
    }

    public Blessing[] getCurrentBlessings() {
        return Arrays.copyOfRange(blessings, 0, this.level);
    }
}
