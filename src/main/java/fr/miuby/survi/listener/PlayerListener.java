package fr.miuby.survi.listener;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import fr.miuby.lib.world.MLWorld;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.role.RoleAttribute;
import fr.miuby.survi.system.SurviConfig;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.system.perf.PerfTimer;
import fr.miuby.survi.world.EWorld;
import io.papermc.paper.advancement.AdvancementDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import fr.miuby.survi.blessing.BlessingEffect;
import fr.miuby.survi.blessing.PotionsEffect;
import fr.miuby.survi.quest.PlayerQuestData;
import fr.miuby.survi.quest.Quest;
import fr.miuby.lib.log.MLLogManager;

import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {

    /** Cooldown d'avertissement « limite du village » par joueur (évite le spam, en ms). */
    private static final long WARN_COOLDOWN_MS = 6_000L;

    private final Map<UUID, Long> lastWarnTime = new HashMap<>();

    // ─── Hot path : mouvement joueur ─────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getPlayer().isOp() || !event.hasChangedPosition()) return;

        try (var t = PerfTimer.start("PlayerListener.onPlayerMove")) {
            Player player    = event.getPlayer();
            String worldName = player.getWorld().getName();

            MLWorld villageWorld    = WorldRegistry.get(EWorld.VILLAGE);
            MLWorld wildernessWorld = WorldRegistry.get(EWorld.WILDERNESS);
            MLWorld netherWorld     = WorldRegistry.get(EWorld.NETHER);

            boolean outOfBounds = false;

            // Village : limite gérée par la zone évolutive
            if (worldName.equals(villageWorld.getName())
                    && GameManager.getInstance().getVillageZoneManager().isLocationOutOfBounds(event.getTo())) {
                outOfBounds = true;
            }

            // Wilderness : limite dynamique selon niveau Aventurier
            if (!outOfBounds && wildernessWorld != null && worldName.equals(wildernessWorld.getWorld().getName())) {
                outOfBounds = isOutOfAdventureLimit(player, event.getTo(), false);
            }

            // Nether : même limite divisée par 8
            if (!outOfBounds && netherWorld != null && worldName.equals(netherWorld.getWorld().getName())) {
                outOfBounds = isOutOfAdventureLimit(player, event.getTo(), true);
            }

            if (outOfBounds) {
                blockMovement(event);
                warn(player);
            }
        }
    }

    // ─── Helpers mouvement ───────────────────────────────────────────────────────

    /**
     * Vérifie si le joueur dépasse la limite d'exploration liée à son niveau Aventurier.
     *
     * @param player  le joueur à tester
     * @param to      la destination du mouvement
     * @param isNether {@code true} pour le Nether (rayon divisé par 8)
     * @return {@code true} si la position est hors des limites autorisées
     */
    private boolean isOutOfAdventureLimit(Player player, Location to, boolean isNether) {
        AlphaPlayer alphaPlayer = AlphaPlayer.get(player.getUniqueId());
        if (alphaPlayer == null) return false;

        int aventurierLevel = alphaPlayer.getJobLevel(EJob.AVENTURIER);
        List<Integer> radii = SurviConfig.getInstance().getAdventureWildernessRadii();

        int idx = Math.min(aventurierLevel, radii.size() - 1);
        int radius = radii.get(idx);
        if (isNether) radius = radius / 8;

        double absX = Math.abs(to.getX());
        double absZ = Math.abs(to.getZ());
        return absX > radius || absZ > radius;
    }

    private void blockMovement(PlayerMoveEvent event) {
        Location from = event.getFrom();

        Location blocked = event.getTo().clone();
        blocked.setX(from.getX());
        blocked.setZ(from.getZ());

        event.setTo(blocked);
    }

    private void warn(Player player) {
        long now = System.currentTimeMillis();

        if (now - lastWarnTime.getOrDefault(player.getUniqueId(), 0L) >= WARN_COOLDOWN_MS) {
            lastWarnTime.put(player.getUniqueId(), now);

            player.sendMessage(Component.text("Tu ne peux pas aller plus loin !", NamedTextColor.RED));
        }
    }

    // ─── Autres events ───────────────────────────────────────────────────────────

    @EventHandler
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        Advancement advancement = event.getAdvancement();
        AdvancementProgress advancementProgress = player.getAdvancementProgress(advancement);
        AdvancementDisplay advancementDisplay = advancement.getDisplay();

        String category = advancement.getKey().getKey().split("/")[0];
        if (advancementProgress.isDone() && advancementDisplay != null && !category.equals("recipes")) {
            MLLogManager.getInstance().log(Level.FINE, ELogTag.PLAYER,
                    "[Advancement] " + player.getName() + " — " + advancement.getKey());
            if (advancementDisplay.frame() == AdvancementDisplay.Frame.CHALLENGE) {
                AlphaPlayer.get(player.getUniqueId()).gainOneSuccess(true);
            }
        }
    }

    @EventHandler
    public void onPlayerArmorChange(PlayerArmorChangeEvent event) {
        MLLogManager.getInstance().log(Level.FINE, ELogTag.PLAYER,
                "[ArmorChange] " + event.getPlayer().getName() + " slot=" + event.getSlotType());
        boolean malus = false;
        for (ItemStack item : event.getPlayer().getInventory().getArmorContents()) {
            if (item != null && GameManager.getInstance().getLockedItemsFactory().isLocked(item.getType().getKey())) {
                malus = true;
            }
        }
        AlphaPlayer.get(event.getPlayer().getUniqueId()).getAlphaLife().setArmorMalus(malus);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        final Player player = event.getEntity();
        MLLogManager.getInstance().log(Level.FINE, ELogTag.PLAYER,
                "[PlayerDeath] " + player.getName() + " mort en " + player.getLocation().getWorld().getName()
                        + " x=" + player.getLocation().getBlockX()
                        + " y=" + player.getLocation().getBlockY()
                        + " z=" + player.getLocation().getBlockZ());

        final ItemStack[] armor = player.getInventory().getArmorContents();
        GameManager.getInstance().getScheduler()
                .scheduleSyncDelayedTask(GameManager.getInstance().getPlugin(),
                        () -> player.getInventory().setArmorContents(armor));

        for (ItemStack is : armor) {
            event.getDrops().remove(is);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        MLLogManager.getInstance().log(Level.FINE, ELogTag.PLAYER,
                "[PlayerRespawn] " + player.getName());
        AlphaPlayer alphaPlayer = AlphaPlayer.get(player.getUniqueId());
        alphaPlayer.getAlphaLife().actualizeDeath();

        for (RoleAttribute roleAttribute : alphaPlayer.getRole().attributes()) {
            if (roleAttribute.getAttributeType() == Attribute.MAX_ABSORPTION) {
                player.removePotionEffect(PotionEffectType.ABSORPTION);
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 0, (int) roleAttribute.getValue()));
            }
        }

        List<BlessingEffect> effectsToReapply = new ArrayList<>();
        for (PlayerQuestData questData : alphaPlayer.getActiveQuests()) {
            if (questData.isClaimed()) {
                Quest quest = GameManager.getInstance().getQuestManager().getQuest(questData.getQuestId());
                if (quest != null) {
                    for (BlessingEffect effect : quest.getRewards().blessingEffects()) {
                        if (effect instanceof PotionsEffect) effectsToReapply.add(effect);
                    }
                }
            }
        }

        if (!effectsToReapply.isEmpty()) {
            GameManager.getInstance().getScheduler().runTaskLater(
                    GameManager.getInstance().getPlugin(),
                    () -> { if (player.isOnline()) effectsToReapply.forEach(e -> e.applyEffect(alphaPlayer)); },
                    5L);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerRecipeDiscover(PlayerRecipeDiscoverEvent event) {
        for (NamespacedKey nsKey : GameManager.getInstance().getCustomRecipeFactory().getOldRecipes()) {
            if (nsKey.toString().equals(event.getRecipe().toString()))
                event.setCancelled(true);
        }
        if (GameManager.getInstance().getLockedItemsFactory().isLocked(event.getRecipe()))
            event.setCancelled(true);
    }
}