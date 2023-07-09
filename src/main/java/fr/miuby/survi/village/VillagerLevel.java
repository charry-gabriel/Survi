package fr.miuby.survi.village;

import fr.miuby.survi.AlphaPlayer;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.blessing.Blessing;
import fr.miuby.survi.Tribute;
import fr.miuby.survi.blessing.BlessingEffect;
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

        GameManager.getInstance().getDatabase().getVillager(this, name);
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

    public void updateInventory() {
        Inventory inv = Bukkit.createInventory(villager, InventoryType.CHEST, Objects.requireNonNull(villager.customName()));

        for (ItemStack item : getTribute().getItemStacks())
            inv.addItem(item);

        this.inventory = inv;
    }

    public void addLevel() {
        this.level++;
        GameManager.getInstance().getDatabase().updateVillager(name, level);
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

    public void setLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return this.level;
    }
}
