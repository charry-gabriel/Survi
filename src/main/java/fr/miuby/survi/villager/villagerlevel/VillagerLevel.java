package fr.miuby.survi.villager.villagerlevel;

import fr.miuby.lib.villager.MLVillagerData;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.GameManager;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.blessing.Blessing;
import fr.miuby.survi.blessing.BlessingEffect;
import fr.miuby.survi.blessing.BlessingLoader;
import fr.miuby.survi.item.SimpleItemStack;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.system.time.TimeManager;
import fr.miuby.survi.villager.AVillager;
import fr.miuby.survi.villager.VillagerConfig;
import fr.miuby.survi.villager.villagerlevel.event.VillagerLevelUpEvent;
import fr.miuby.survi.world.WorldInitializer;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class VillagerLevel extends AVillager {
    private Tribute[]       tributes;
    private TextComponent[] names;
    private TextComponent[] recapMessages;
    private Blessing[]      blessings;
    /** locks[N] est appliqué quand le niveau N est complété. Null = pas de lock. */
    private Duration[]      locks;
    @Getter private @Nullable UUID skinUuid;

    @Getter @Setter
    private int level = 0;
    private final List<ItemStack> givenItems = new ArrayList<>();
    private Instant unlockedInstant = Instant.EPOCH;

    public VillagerLevel(String nameId, @Nullable UUID skinUuid,
                         Blessing[] blessings, Duration[] locks, TextComponent[] messages,
                         Tribute[] tributes, TextComponent[] names, TextComponent[] recap) {
        super(nameId, messages);
        this.skinUuid = skinUuid;
        this.blessings = blessings;
        this.locks = locks;
        this.tributes = tributes;
        this.names = names;
        this.recapMessages = recap;
    }

    // =========================================================================
    // EntityType
    // =========================================================================

    @Override
    protected EntityType getEntityType() {
        return EntityType.MANNEQUIN;
    }

    // =========================================================================
    // Initialisation — skin Mannequin
    // =========================================================================

    @Override
    protected void onInitialized() {
        if (getVillager() instanceof Mannequin mannequin) {
            mannequin.setImmovable(true);
            mannequin.setDescription(null); // cache le label "NPC" affiché par défaut
            if (skinUuid != null) {
                try {
                    ResolvableProfile profile = ResolvableProfile.resolvableProfile().uuid(skinUuid).build();
                    mannequin.setProfile(profile);
                } catch (IllegalArgumentException e) {
                    MLLogManager.getInstance().log(Level.WARNING, ELogTag.VILLAGER,
                            nameId + " : skin invalide — UUID attendu, valeur : \"" + skinUuid + "\"");
                }
            }
        }
        super.onInitialized();
    }

    // =========================================================================
    // Reload à chaud
    // =========================================================================

    /**
     * Recharge la configuration de ce villageois depuis le {@link VillagerConfig} fourni,
     * sans recréer l'entité Bukkit ni toucher la progression des joueurs.
     */
    public void reloadConfig(VillagerConfig config) {
        this.names         = config.levels.stream().map(l -> Component.text(l.name)).toArray(TextComponent[]::new);
        this.messages      = config.levels.stream().map(l -> Component.text(l.message)).toArray(TextComponent[]::new);
        this.recapMessages = config.levels.stream().map(l -> Component.text(l.recap)).toArray(TextComponent[]::new);
        this.tributes      = config.levels.stream()
                .map(l -> new Tribute(l.tribute.stream().map(SimpleItemStack::toItemStack).toArray(ItemStack[]::new)))
                .toArray(Tribute[]::new);
        this.blessings     = config.levels.stream()
                .map(l -> BlessingLoader.loadFromList(this.nameId, l.blessings))
                .map(b -> b != null ? b : new Blessing(new BlessingEffect[0]))
                .toArray(Blessing[]::new);
        this.locks         = config.levels.stream()
                .map(l -> l.lock != null ? Duration.ofDays(l.lock) : null)
                .toArray(Duration[]::new);

        this.skinUuid = UUID.fromString(config.skin);

        if (getVillager() != null) {
            getVillager().customName(getDisplayName());
            if (getVillager() instanceof Mannequin mannequin && skinUuid != null) {
                try {
                    ResolvableProfile profile = ResolvableProfile.resolvableProfile().uuid(skinUuid).build();
                    mannequin.setProfile(profile);
                } catch (IllegalArgumentException e) {
                    MLLogManager.getInstance().log(Level.WARNING, ELogTag.VILLAGER,
                            nameId + " : skin invalide — UUID attendu, valeur : \"" + skinUuid + "\"");
                }
            }
            refreshInventoryContent();
        }

        MLLogManager.getInstance().log(Level.INFO, ELogTag.VILLAGER,
                "[Reload] " + nameId + " rechargé (" + config.levels.size() + " niveaux).");
    }

    /**
     * Reconstruit le contenu de l'inventaire existant avec le nouveau tribute
     * en déduisant les items déjà remis par les joueurs pour le niveau actuel.
     */
    public void refreshInventoryContent() {
        if (this.inventory == null) return;
        this.inventory.clear();

        Tribute tribute = getTribute();
        if (tribute == null) return;

        for (ItemStack item : tribute.getItemStacks())
            this.inventory.addItem(item);

        for (ItemStack given : givenItems)
            createItemStack(this.inventory, new ItemStack(given));
    }

    // =========================================================================
    // Données persistées
    // =========================================================================

    @Override
    protected AlphaVillagerData createDefaultData() {
        return new AlphaVillagerData(null, nameId,
                new Location(WorldInitializer.getDefaultWorld(), 0, 700, 0),
                new ArrayList<>(), 0, Instant.EPOCH.toEpochMilli());
    }

    @Override
    protected @Nullable MLVillagerData loadData() {
        AlphaVillagerData data = GameManager.getInstance().getDatabase().villagers().load(this.getNameId());
        if (data != null) {
            this.level = data.getLevel();
            this.givenItems.addAll(data.getGivenItems());
            if (data.getUnlockToEpochMilli() != null)
                this.unlockedInstant = Instant.ofEpochMilli(data.getUnlockToEpochMilli());
        }
        return data;
    }

    // =========================================================================
    // Inventaire tribut
    // =========================================================================

    @Override
    public void createInventory() {
        VillagerTributeHolder holder = new VillagerTributeHolder(this);
        Inventory inv = Bukkit.createInventory(holder, InventoryType.CHEST, Objects.requireNonNull(getDisplayName()));

        Tribute tribute = getTribute();
        if (tribute == null) {
            this.inventory = inv;
            return;
        }

        for (ItemStack item : tribute.getItemStacks())
            inv.addItem(item);

        for (ItemStack item : givenItems)
            createItemStack(inv, new ItemStack(item));

        this.inventory = inv;
    }

    // =========================================================================
    // Logique tribute
    // =========================================================================

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

    /**
     * Remet le villager au niveau 0 et annule tous les effets de blessings
     * qui avaient été appliqués. Appelé depuis la commande /villager <id> reset.
     */
    public void resetLevel() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (Blessing blessing : getCurrentBlessings()) {
                for (BlessingEffect effect : blessing.blessingEffects()) {
                    effect.resetEffect(AlphaPlayer.get(p.getUniqueId()));
                }
            }
        }

        this.level = 0;
        GameManager.getInstance().getDatabase().villagers().updateLevel(getVillager().getUniqueId(), 0);

        this.givenItems.clear();
        GameManager.getInstance().getDatabase().villagers().updateGivenItems(getVillager().getUniqueId(), this.givenItems);

        this.unlockedInstant = Instant.EPOCH;
        GameManager.getInstance().getDatabase().villagers().updateLock(getVillager().getUniqueId(), null);

        getVillager().customName(getDisplayName());
        updateInventory();

        MLLogManager.getInstance().log(Level.INFO, ELogTag.VILLAGER, nameId + " a été reset au niveau 0.");
    }

    public boolean levelUp() {
        if (isMaxLevel()) {
            MLLogManager.getInstance().log(Level.INFO, ELogTag.VILLAGER, nameId + " est déjà au niveau maximum");
            return false;
        }

        int completedLevel = this.level;

        VillagerLevelUpEvent event = new VillagerLevelUpEvent(this, this.level + 1);
        Bukkit.getPluginManager().callEvent(event);

        this.level++;
        GameManager.getInstance().getDatabase().villagers().updateLevel(getVillager().getUniqueId(), level);

        getVillager().customName(getDisplayName());
        updateInventory();

        this.givenItems.clear();
        GameManager.getInstance().getDatabase().villagers().updateGivenItems(getVillager().getUniqueId(), this.givenItems);

        if (locks != null && completedLevel < locks.length && locks[completedLevel] != null) {
            lock(locks[completedLevel]);
        }

        return true;
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

    public void applyAllCurrentBlessing(AlphaPlayer player) {
        player.getAlphaLife().regenHealth(() -> {
            for (Blessing blessing : getCurrentBlessings()) {
                for (BlessingEffect effect : blessing.blessingEffects()) {
                    effect.applyEffect(player);
                }
            }
        });
    }

    private void createItemStack(Inventory inventory, ItemStack item) {
        ItemStack itemToRemove = null;

        for (ItemStack tributeItem : inventory.getContents()) {
            if (tributeItem != null && tributeItem.isSimilar(item)) {
                MLLogManager.getInstance().log(Level.INFO, ELogTag.VILLAGER,
                        this.nameId + " recupere " + item.getAmount() + " de " + item.getType().name());

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
                MLLogManager.getInstance().log(Level.INFO, ELogTag.VILLAGER,
                        this.nameId + " recupere " + item.getAmount() + " de " + item.getType().name());
                this.givenItems.add(new ItemStack(item));
                GameManager.getInstance().getDatabase().villagers().updateGivenItems(getVillager().getUniqueId(), this.givenItems);

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

    // =========================================================================
    // Accesseurs
    // =========================================================================

    public Tribute getTribute() {
        if (this.level >= tributes.length) return null;

        if (tributes[this.level].getItemStacks().isEmpty()) {
            if (this.level + 1 != tributes.length)
                MLLogManager.getInstance().log(Level.WARNING, ELogTag.VILLAGER,
                        "Le niveau " + this.level + " de " + this.nameId + " n'a pas de tributes !");
            return null;
        }

        return tributes[this.level];
    }

    public Blessing getBlessing() { return blessings[this.level]; }

    public TextComponent getMessage() { return messages[this.level].color(NamedTextColor.AQUA); }

    @Override
    public TextComponent getDisplayName() { return names[this.level].color(NamedTextColor.AQUA); }

    public Blessing[] getCurrentBlessings() { return Arrays.copyOfRange(blessings, 0, this.level); }

    public TextComponent getRecapMessage() { return recapMessages[this.level].color(NamedTextColor.AQUA); }

    // =========================================================================
    // Lock / unlock
    // =========================================================================

    public boolean isUnlocked() { return Instant.now().isAfter(unlockedInstant); }

    public void lock(Duration duration) {
        if (this.isUnlocked()) {
            this.unlockedInstant = Instant.now().plus(duration);
            GameManager.getInstance().getDatabase().villagers().updateLock(getVillager().getUniqueId(), this.unlockedInstant.toEpochMilli());
        }
    }

    public boolean unlock() {
        if (isUnlocked()) {
            MLLogManager.getInstance().log(Level.WARNING, ELogTag.VILLAGER, getNameId() + " déjà débloqué");
            return false;
        }
        unlockedInstant = Instant.EPOCH;
        GameManager.getInstance().getDatabase().villagers().updateLock(getVillager().getUniqueId(), null);
        return true;
    }

    public String getRemainingLock() {
        if (unlockedInstant == null || unlockedInstant.equals(Instant.EPOCH)) return "Débloqué";
        Duration remaining = Duration.between(Instant.now(), unlockedInstant);
        return remaining.isNegative() ? "Débloqué" : TimeManager.formatTime(remaining);
    }
}