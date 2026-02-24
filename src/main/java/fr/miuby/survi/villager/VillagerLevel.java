package fr.miuby.survi.villager;

import fr.miuby.lib.villager.MLVillagerData;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.log.LogManager;
import fr.miuby.survi.villager.blessing.Blessing;
import fr.miuby.survi.villager.blessing.BlessingEffect;
import fr.miuby.survi.villager.event.VillagerLevelUpEvent;
import fr.miuby.survi.world.WorldInitializer;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class VillagerLevel extends AVillager {
    private final Tribute[] tributes;
    private final TextComponent[] names;
    private final TextComponent[] recapMessages;
    private final Blessing[] blessings;

    @Getter
    @Setter
    private int level = 0;
    private final List<ItemStack> givenItems = new ArrayList<>();

    private Instant unlockedDate;
    @Getter
    private BukkitTask unlockTask;

    public VillagerLevel(String nameId, Villager.Type type, Villager.Profession profession, Blessing[] blessings, TextComponent[] messages, Tribute[] tributes, TextComponent[] names, TextComponent[] recap) {
        super(nameId, type, profession, messages);
        this.blessings = blessings;
        this.tributes = tributes;
        this.names = names;
        this.recapMessages = recap;
    }

    @Override
    protected AlphaVillagerData createDefaultData() {
        return new AlphaVillagerData(null, nameId, new Location(WorldInitializer.getDefaultWorld(), 0, 700, 0), new ArrayList<>(), 0, null);
    }

    @Override
    protected @Nullable MLVillagerData loadData() {
        AlphaVillagerData data = GameManager.getInstance().getDatabase().initVillager(this.getNameId());
        if (data != null) {
            this.level = data.getLevel();
            this.givenItems.addAll(data.getGivenItems());

            if (data.getUnlockedDate() != null)
                this.unlockedDate = Instant.ofEpochMilli(data.getUnlockedDate());
        }
        return data;
    }

    public void giveItems(Inventory inventory, ItemStack item, Player player) {
        removeItemStack(inventory, item, player);

        if (inventory.isEmpty()) {
            levelUp();
            player.closeInventory();
        }
    }

    public boolean isMaxLevel() {
        return level >= Math.max(
                Math.max(tributes.length, blessings.length),
                Math.max(names.length, Math.max(messages.length, recapMessages.length))
        ) - 1;
    }

    public boolean levelUp() {
        if (isMaxLevel()) {
            LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.VILLAGER, nameId + " est déjà au niveau maximum");
            return false;
        }

        VillagerLevelUpEvent event = new VillagerLevelUpEvent(this, this.level + 1);
        Bukkit.getPluginManager().callEvent(event);

        this.level++;
        GameManager.getInstance().getDatabase().updateVillagerLevel(getVillager().getUniqueId(), level);

        getVillager().customName(getDisplayName());
        updateInventory();

        this.givenItems.clear();
        GameManager.getInstance().getDatabase().updateVillagerGivenItem(getVillager().getUniqueId(), this.givenItems);

        return true;
    }

    public void createInventory() {
        Inventory inv = Bukkit.createInventory(getVillager(), InventoryType.CHEST, Objects.requireNonNull(getVillager().customName()));

        Tribute tribute = getTribute();
        if (tribute == null)
            return;

        for (ItemStack item : tribute.getItemStacks())
            inv.addItem(item);

        for (ItemStack item : givenItems) {
            createItemStack(inv, new ItemStack(item));
        }

        this.inventory = inv;
    }

    public void updateInventory() {
        if (this.inventory == null) return;
        this.inventory.clear();

        Tribute tribute = getTribute();
        if (tribute != null) {
            for (ItemStack item : tribute.getItemStacks())
                this.inventory.addItem(item);
        }
    }

    public void applyAllCurrentBlessing(VillagerLevel villager, AlphaPlayer player) {
        player.getAlphaLife().regenHealth(() -> {
            for (Blessing blessing : getCurrentBlessings()) {
                for (BlessingEffect effect : blessing.blessingEffects()) {
                    effect.applyEffect(villager, player);
                }
            }
        });
    }

    private void createItemStack(Inventory inventory, ItemStack item) {
        ItemStack itemToRemove = null;

        for (ItemStack tributeItem : inventory.getContents()) {
            if (tributeItem != null && tributeItem.isSimilar(item)) {
                LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.VILLAGER, this.nameId + " recupere " + item.getAmount() + " de " + item.getType().name());

                if (item.getAmount() < tributeItem.getAmount()) {
                    tributeItem.setAmount(tributeItem.getAmount() - item.getAmount());
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

    public void removeItemStack(Inventory inventory, ItemStack item, Player player) {
        ItemStack itemToRemove = null;

        for (ItemStack tributeItem : inventory.getContents()) {
            if (tributeItem != null && tributeItem.isSimilar(item)) {
                LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.VILLAGER, this.nameId + " recupere " + item.getAmount() + " de " + item.getType().name());
                this.givenItems.add(new ItemStack(item));
                GameManager.getInstance().getDatabase().updateVillagerGivenItem(getVillager().getUniqueId(), this.givenItems);

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
        if (this.level >= tributes.length || tributes[this.level].getItemStacks().isEmpty())
            return null;

        return tributes[this.level];
    }

    public Blessing getBlessing() {
        return blessings[this.level];
    }

    public TextComponent getMessage() {
        return messages[this.level].color(NamedTextColor.AQUA);
    }

    @Override
    public TextComponent getDisplayName() {
        return names[this.level].color(NamedTextColor.AQUA);
    }

    public Blessing[] getCurrentBlessings() {
        return Arrays.copyOfRange(blessings, 0, this.level);
    }

    public TextComponent getRecapMessage() {
        return recapMessages[this.level].color(NamedTextColor.AQUA);
    }

    //region unlock
    public boolean isUnlocked() {
        if (unlockedDate == null) return false;
        return Instant.now().isAfter(unlockedDate);
    }

    public void cancelUnlockTask() {
        if (unlockTask != null && !unlockTask.isCancelled()) {
            unlockTask.cancel();
            unlockTask = null;
        }
    }

    public void lock(Duration duration) {
        if (this.isUnlocked()) {
            this.unlockedDate = Instant.now().plus(duration);
            cancelUnlockTask();

            unlockTask = Bukkit.getScheduler().runTaskLater(GameManager.getInstance().getPlugin(), this::handleUnlockTask, duration.getSeconds() * 20);
            GameManager.getInstance().getDatabase().lockVillager(getVillager().getUniqueId(), this.unlockedDate.toEpochMilli());
        }
    }

    public void handleUnlockTask() {
        if (isUnlocked()) {
            LogManager.getInstance().log(Level.WARNING, LogManager.ETagLog.VILLAGER, "Task unlock déclenchée mais " + getNameId() + " déjà débloqué");
            return;
        }

        cancelUnlockTask();

        unlockedDate = null;
        GameManager.getInstance().getDatabase().lockVillager(getVillager().getUniqueId(), null);
    }

    public String getUnlockedDate() {
        if (unlockedDate.getEpochSecond() == 0) return "N/A";
        return unlockedDate.toString();
    }
    //endregion
}
