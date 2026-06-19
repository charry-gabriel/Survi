package fr.miuby.survi.listener;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import fr.miuby.lib.utils.Cooldown;
import fr.miuby.lib.utils.Rect;
import fr.miuby.lib.world.MLWorld;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.lib.world.WorldType;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.role.RoleAttribute;
import fr.miuby.survi.system.SurviConfig;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.system.perf.PerfTimer;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.zone.ZoneBounds;
import io.papermc.paper.advancement.AdvancementDisplay;
import fr.miuby.survi.blessing.BlessingEffect;
import fr.miuby.survi.blessing.PotionsEffect;
import fr.miuby.survi.quest.quest.PlayerQuestData;
import fr.miuby.survi.quest.quest.Quest;
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
import java.util.List;
import java.util.UUID;

public class PlayerListener implements Listener {

    /** Cooldown d'avertissement « limite du village » par joueur (évite le spam, en ms). */
    private static final long WARN_COOLDOWN_MS = 6_000L;

    // ─── Références stables pré-cachées ──────────────────────────────────────────

    private final GameManager gm;
    private final SurviConfig surviConfig;

    /** Cooldown d'avertissement par joueur — remplace l'ancien Map<UUID, Long>. */
    private final Cooldown<UUID> warnCooldown = new Cooldown<>(WARN_COOLDOWN_MS);

    public PlayerListener() {
        this.gm          = GameManager.getInstance();
        this.surviConfig = SurviConfig.getInstance();
    }

    // ─── Hot path : mouvement joueur ─────────────────────────────────────────────

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getPlayer().isOp() || !event.hasChangedPosition()) return;

        try (var t = PerfTimer.start("PlayerListener.onPlayerMove")) {
            Player player = event.getPlayer();

            MLWorld mlWorld = WorldRegistry.get(player.getWorld().getUID());
            if (mlWorld == null) return;

            WorldType worldType = mlWorld.getType();
            boolean outOfBounds;
            if (worldType == EWorld.VILLAGE) {
                Rect limit = mlWorld.getLimit();
                if (limit != null && event.getTo().getY() < limit.yMin()) {
                    player.teleport(mlWorld.getWorld().getSpawnLocation());
                    return;
                }
                outOfBounds = gm.getVillageZoneManager().isLocationOutOfBounds(event.getTo());
            } else if (worldType == EWorld.WILDERNESS) {
                outOfBounds = isOutOfExploreLimit(player, event.getTo(), false);
            } else if (worldType == EWorld.NETHER) {
                outOfBounds = isOutOfExploreLimit(player, event.getTo(), true);
            } else {
                return;
            }

            if (outOfBounds) {
                blockMovement(event);
                warn(player);
            }
        }
    }

    // ─── Helpers mouvement ───────────────────────────────────────────────────────

    /**
     * Vérifie si le joueur dépasse la limite d'exploration liée à son niveau Explorateur.
     *
     * @param player   le joueur à tester
     * @param to       la destination du mouvement
     * @param isNether {@code true} pour le Nether (rayon divisé par 8)
     * @return {@code true} si la position est hors des limites autorisées
     */
    private boolean isOutOfExploreLimit(Player player, Location to, boolean isNether) {
        AlphaPlayer alphaPlayer = AlphaPlayer.get(player.getUniqueId());
        if (alphaPlayer == null) return false;

        List<Integer> radii = surviConfig.getExploreWildernessRadius();
        int idx    = Math.min(alphaPlayer.getJobLevel(EJob.EXPLORER), radii.size() - 1);
        int radius = radii.get(idx);
        if (isNether) radius = radius / 8;

        return new ZoneBounds(0, 0, radius, radius).isOutside(to);
    }

    private void blockMovement(PlayerMoveEvent event) {
        Location from = event.getFrom();

        Location blocked = event.getTo().clone();
        blocked.setX(from.getX());
        blocked.setZ(from.getZ());

        event.setTo(blocked);
    }

    private void warn(Player player) {
        UUID uuid = player.getUniqueId();
        if (!warnCooldown.isOnCooldown(uuid)) {
            warnCooldown.set(uuid);
            player.sendMessage(gm.getLangService().text(player, "boundary.warning"));
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
            if (item != null && gm.getLockedItemsFactory().isLocked(item.getType().getKey())) {
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

        gm.getScheduler().scheduleSyncDelayedTask(gm.getPlugin(), () -> player.getInventory().setArmorContents(armor));

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

        event.setRespawnLocation(GameManager.getInstance().getVillageZoneManager().getCurrentSpawnLocation());

        for (RoleAttribute roleAttribute : alphaPlayer.getRole().attributes()) {
            if (roleAttribute.getAttributeType() == Attribute.MAX_ABSORPTION) {
                player.removePotionEffect(PotionEffectType.ABSORPTION);
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 0, (int) roleAttribute.getValue()));
            }
        }

        List<BlessingEffect> effectsToReapply = new ArrayList<>();
        for (PlayerQuestData questData : alphaPlayer.getActiveQuests()) {
            if (questData.isClaimed()) {
                Quest quest = gm.getQuestManager().getQuest(questData.getQuestId());
                if (quest != null) {
                    for (BlessingEffect effect : quest.getRewards().blessingEffects()) {
                        if (effect instanceof PotionsEffect) effectsToReapply.add(effect);
                    }
                }
            }
        }

        if (!effectsToReapply.isEmpty()) {
            gm.getScheduler().runTaskLater(
                    gm.getPlugin(),
                    () -> { if (player.isOnline()) effectsToReapply.forEach(e -> e.applyEffect(alphaPlayer)); },
                    5L);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerRecipeDiscover(PlayerRecipeDiscoverEvent event) {
        for (NamespacedKey nsKey : gm.getCustomRecipeFactory().getOldRecipes()) {
            if (nsKey.toString().equals(event.getRecipe().toString()))
                event.setCancelled(true);
        }
        if (gm.getLockedItemsFactory().isLocked(event.getRecipe()))
            event.setCancelled(true);
    }
}