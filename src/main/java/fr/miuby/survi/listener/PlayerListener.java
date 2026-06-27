package fr.miuby.survi.listener;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent;
import fr.miuby.lib.utils.Cooldown;
import fr.miuby.lib.utils.Rect;
import fr.miuby.lib.world.MLWorld;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.lib.world.WorldType;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.job.config.JobsConfig;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.role.RoleAttribute;
import fr.miuby.survi.system.lang.LangService;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.system.perf.PerfTimer;
import fr.miuby.survi.world.EWorld;
import fr.miuby.survi.world.zone.ZoneBounds;
import io.papermc.paper.advancement.AdvancementDisplay;
import fr.miuby.survi.blessing.BlessingEffect;
import fr.miuby.survi.blessing.FlyEffect;
import fr.miuby.survi.blessing.PotionsEffect;
import fr.miuby.survi.quest.quest.PlayerQuestData;
import fr.miuby.survi.quest.quest.Quest;
import fr.miuby.lib.log.MLLogManager;

import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
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

    // ─── Références stables pré-cachées ──────────────────────────────────────────

    private final GameManager gm;

    /** Cooldown d'avertissement par joueur — remplace l'ancien Map<UUID, Long>. */
    private final Cooldown<UUID> warnCooldown = new Cooldown<>(WARN_COOLDOWN_MS);

    /** Armure sauvegardée à la mort, restituée après le respawn (pas pendant l'écran de mort). */
    private final Map<UUID, ItemStack[]> pendingArmorRestore = new HashMap<>();

    /**
     * Timestamp d'expiration du cooldown de respawn par joueur (ms depuis epoch).
     * Absent = pas de cooldown actif.
     * Persiste en mémoire le temps de la session serveur — pas nettoyé au quit intentionnellement
     * pour empêcher le bypass par reconnexion rapide.
     */
    private final Map<UUID, Long> respawnCooldownExpiry = new HashMap<>();

    public PlayerListener() {
        this.gm = GameManager.getInstance();
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
                    warn(player);
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

        int radius = JobsConfig.getInstance().getExplorer()
                .wildernessRadiusForLevel(alphaPlayer.getJobLevel(EJob.EXPLORER), isNether);

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

        AlphaPlayer.get(player.getUniqueId()).getAlphaLife().saveFood();

        final ItemStack[] armor = player.getInventory().getArmorContents();
        pendingArmorRestore.put(player.getUniqueId(), armor);
        MLLogManager.getInstance().log(Level.FINE, ELogTag.PLAYER,
                "[PlayerDeath] " + player.getName() + " — armure sauvegardée (" + armor.length + " slots)");

        for (ItemStack is : armor) {
            event.getDrops().remove(is);
        }
    }

    @EventHandler
    public void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        Location loc = event.getLocation();
        Player player = event.getPlayer();
        AlphaPlayer ap = AlphaPlayer.get(player.getUniqueId());
        LangService ls = gm.getLangService();

        if (loc == null) {
            // Spawn effacé (lit détruit, etc.)
            ap.setCustomSpawnLocation(null);
            gm.getDatabase().players().clearSpawnLocation(player.getUniqueId());
            MLLogManager.getInstance().log(Level.FINE, ELogTag.PLAYER,
                    "[SetSpawn] " + player.getName() + " spawn effacé (cause=" + event.getCause() + ")");
            return;
        }

        PlayerSetSpawnEvent.Cause cause = event.getCause();
        if (cause != PlayerSetSpawnEvent.Cause.BED && cause != PlayerSetSpawnEvent.Cause.RESPAWN_ANCHOR) return;

        event.setNotifyPlayer(false);

        ap.setCustomSpawnLocation(loc.clone());
        gm.getDatabase().players().saveSpawnLocation(player.getUniqueId(), loc);

        if (cause == PlayerSetSpawnEvent.Cause.BED) {
            player.sendMessage(ls.text(player, "spawn.set.bed"));
        } else {
            player.sendMessage(ls.text(player, "spawn.set.anchor"));
        }
        MLLogManager.getInstance().log(Level.FINE, ELogTag.PLAYER,
                "[SetSpawn] " + player.getName() + " → " + loc.getWorld().getName()
                        + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()
                        + " (cause=" + cause + ")");
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        AlphaPlayer alphaPlayer = AlphaPlayer.get(player.getUniqueId());
        alphaPlayer.getAlphaLife().actualizeDeath();
        gm.getScheduler().scheduleSyncDelayedTask(gm.getPlugin(), () -> { if (player.isOnline()) alphaPlayer.getAlphaLife().restoreFood(); });

        // ─── Logique de respawn : spawn perso (lit/ancre) vs spawn village ────────
        Location customSpawn = alphaPlayer.getCustomSpawnLocation();
        long now = System.currentTimeMillis();
        Long expiryTs = respawnCooldownExpiry.get(player.getUniqueId());
        boolean onCooldown = expiryTs != null && now < expiryTs;

        if (customSpawn != null && !onCooldown) {
            event.setRespawnLocation(customSpawn);
            int lumberjackLevel = alphaPlayer.getJobLevel(EJob.LUMBERJACK);
            long cooldownMs = JobsConfig.getInstance().getLumberjack().getRespawnCooldownSeconds()[lumberjackLevel] * 1000L;
            respawnCooldownExpiry.put(player.getUniqueId(), now + cooldownMs);
            MLLogManager.getInstance().log(Level.INFO, ELogTag.PLAYER,
                    "[PlayerRespawn] " + player.getName() + " → spawn perso ("
                            + customSpawn.getWorld().getName() + " " + customSpawn.getBlockX()
                            + " " + customSpawn.getBlockY() + " " + customSpawn.getBlockZ()
                            + ") cooldown=" + (cooldownMs / 1000) + "s (bucheron niv." + lumberjackLevel + ")");
        } else {
            event.setRespawnLocation(GameManager.getInstance().getVillageZoneManager().getCurrentSpawnLocation());
            if (onCooldown && customSpawn != null) {
                long remaining = expiryTs - now;
                gm.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                    if (player.isOnline()) player.sendMessage(gm.getLangService().text(player, "respawn.cooldown.village", formatDuration(remaining)));
                }, 1L);
                MLLogManager.getInstance().log(Level.INFO, ELogTag.PLAYER,
                        "[PlayerRespawn] " + player.getName() + " → spawn village (cooldown actif, restant=" + (remaining / 1000) + "s)");
            } else {
                MLLogManager.getInstance().log(Level.FINE, ELogTag.PLAYER,
                        "[PlayerRespawn] " + player.getName() + " → spawn village (pas de spawn personnalisé)");
            }
        }
        // ─────────────────────────────────────────────────────────────────────────

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

        ItemStack[] pendingArmor = pendingArmorRestore.remove(player.getUniqueId());
        if (pendingArmor != null) {
            gm.getScheduler().runTaskLater(gm.getPlugin(), () -> {
                if (player.isOnline()) {
                    player.getInventory().setArmorContents(pendingArmor);
                    MLLogManager.getInstance().log(Level.FINE, ELogTag.PLAYER,
                            "[PlayerRespawn] " + player.getName() + " — armure restaurée");
                }
            }, 1L);
        }
    }

    /**
     * Affiche le temps restant de cooldown de respawn quand le joueur fait un clic droit
     * sur un lit ou une ancre de respawn — uniquement s'il a un spawn personnalisé défini.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractRespawnBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        Material type = block.getType();
        if (!Tag.BEDS.isTagged(type) && type != Material.RESPAWN_ANCHOR) return;

        Player player = event.getPlayer();
        AlphaPlayer ap = AlphaPlayer.get(player.getUniqueId());
        if (ap.getCustomSpawnLocation() == null) return;

        long now = System.currentTimeMillis();
        Long expiryTs = respawnCooldownExpiry.get(player.getUniqueId());
        boolean onCooldown = expiryTs != null && now < expiryTs;

        if (onCooldown) {
            long remaining = expiryTs - now;
            player.sendActionBar(gm.getLangService().text(player, "respawn.cooldown.actionbar", formatDuration(remaining)));
            MLLogManager.getInstance().log(Level.FINE, ELogTag.PLAYER,
                    "[RespawnBlock] " + player.getName() + " inspecte bloc respawn — cooldown restant=" + (remaining / 1000) + "s");
        } else {
            player.sendActionBar(gm.getLangService().text(player, "respawn.cooldown.ready"));
            MLLogManager.getInstance().log(Level.FINE, ELogTag.PLAYER,
                    "[RespawnBlock] " + player.getName() + " inspecte bloc respawn — prêt");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        pendingArmorRestore.remove(event.getPlayer().getUniqueId());
    }

    // ─── Changement de monde : gestion du fly Village ────────────────────────────

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        var mlWorld = WorldRegistry.get(player.getWorld().getUID());
        if (mlWorld == null) return;

        if (mlWorld.getType() == EWorld.VILLAGE) {
            if (FlyEffect.isActive()) {
                FlyEffect.enableFly(player);
            }
        } else if (player.getAllowFlight()) {
            FlyEffect.disableFly(player);
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

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    /** Formate une durée en ms en chaîne lisible : « 1h 23m », « 45m 10s » ou « 30s ». */
    private static String formatDuration(long ms) {
        long s = ms / 1000;
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + sec + "s";
        return sec + "s";
    }
}