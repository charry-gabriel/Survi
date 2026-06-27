package fr.miuby.survi.item.growth_item;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.item.ECustomItem;
import fr.miuby.survi.item.growth_item.config.GrowthConfig;
import fr.miuby.survi.item.growth_item.effect.ItemEffect;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.command.argument.AlphaPlayerArgument;
import fr.miuby.survi.system.command.argument.GrowthItemArgument;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.system.exception.AlphaPlayerNotFoundException;
import fr.miuby.survi.system.lang.LangService;
import fr.miuby.survi.system.log.ELogTag;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Commandes d'administration pour les growth items.
 *
 * <pre>
 * /growthitem give       {@code <player> <growthId> [level]} — donne l'item au palier indiqué (défaut : 0, item frais)
 * /growthitem lvlup      {@code <player> [growthId]}         — force le palier suivant (item en main si sans growthId)
 * /growthitem info       {@code <player>}                    — liste tous les growth items dans l'inventaire du joueur
 * /growthitem track      {@code <growthId>}                  — joueurs en ligne possédant cet item (inventaire inclus armure)
 * /growthitem remove     {@code <player> <growthId>}         — retire tous les exemplaires de l'item de l'inventaire
 * /growthitem replace    {@code <player> <growthId>}         — remplace l'item par la version ECustomItem courante en conservant uses/tier
 * /growthitem replaceall {@code <growthId>}                  — idem pour tous les joueurs en ligne
 * </pre>
 *
 * <p>⚠ {@code track}, {@code remove}, {@code replace} et {@code replaceall} ne fonctionnent que sur les joueurs <b>en ligne</b> :
 * les PDC sont portés par l'item physique, non persistés en base.</p>
 */
@SuppressWarnings({"java:S3516", "SameReturnValue"})
public class GrowthItemCommand {

    private GrowthItemCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("growthitem")
                .requires(source -> source.getSender().isOp())

                // /growthitem give <player> <growthId> [level]
                .then(Commands.literal("give")
                        .then(Commands.argument("player", AlphaPlayerArgument.alphaPlayer())
                                .then(Commands.argument("growthId", GrowthItemArgument.growthItem())
                                        .executes(ctx -> giveItem(ctx, 0))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                                .executes(ctx -> giveItem(ctx, IntegerArgumentType.getInteger(ctx, "level")))
                                        )
                                )
                        )
                )

                // /growthitem lvlup <player> [growthId]
                .then(Commands.literal("lvlup")
                        .then(Commands.argument("player", AlphaPlayerArgument.alphaPlayer())
                                .executes(GrowthItemCommand::lvlUpHeld)
                                .then(Commands.argument("growthId", GrowthItemArgument.growthItem())
                                        .executes(GrowthItemCommand::lvlUpById)
                                )
                        )
                )

                // /growthitem info <player>
                .then(Commands.literal("info")
                        .then(Commands.argument("player", AlphaPlayerArgument.alphaPlayer())
                                .executes(GrowthItemCommand::showInfo)
                        )
                )

                // /growthitem track <growthId>
                .then(Commands.literal("track")
                        .then(Commands.argument("growthId", GrowthItemArgument.growthItem())
                                .executes(GrowthItemCommand::trackItem)
                        )
                )

                // /growthitem remove <player> <growthId>
                .then(Commands.literal("remove")
                        .then(Commands.argument("player", AlphaPlayerArgument.alphaPlayer())
                                .then(Commands.argument("growthId", GrowthItemArgument.growthItem())
                                        .executes(GrowthItemCommand::removeItem)
                                )
                        )
                )

                // /growthitem replace <player> <growthId>
                .then(Commands.literal("replace")
                        .then(Commands.argument("player", AlphaPlayerArgument.alphaPlayer())
                                .then(Commands.argument("growthId", GrowthItemArgument.growthItem())
                                        .executes(GrowthItemCommand::replaceItem)
                                )
                        )
                )

                // /growthitem replaceall <growthId>
                .then(Commands.literal("replaceall")
                        .then(Commands.argument("growthId", GrowthItemArgument.growthItem())
                                .executes(GrowthItemCommand::replaceAll)
                        )
                );
    }

    // ─── give ─────────────────────────────────────────────────────────────────

    private static int giveItem(CommandContext<CommandSourceStack> ctx, int targetLevel) {
        AlphaPlayer alpha = AlphaPlayerArgument.getAlphaPlayer(ctx, "player");
        String growthId = GrowthItemArgument.getId(ctx, "growthId");
        Player target = alpha.getPlayer();
        var sender = ctx.getSource().getSender();

        GrowthConfig config = GrowthItemRegistry.get(growthId);
        int maxLevel = config.tiers().size();

        LangService ls = GameManager.getInstance().getLangService();
        if (targetLevel > maxLevel) {
            sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.growth.level_too_high", growthId, maxLevel));
            return Command.SINGLE_SUCCESS;
        }

        ECustomItem customItem = ECustomItem.fromString(growthId.toLowerCase());
        if (customItem == null) {
            sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.growth.no_item", growthId));
            return Command.SINGLE_SUCCESS;
        }

        ItemStack item = customItem.getItemStack().clone();

        // Applique tous les paliers de 0 à targetLevel - 1 en accumulant les effets sur l'item
        for (int i = 0; i < targetLevel; i++) {
            GrowthTier tier = config.tiers().get(i);
            ItemMeta meta = item.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(GrowthItems.TIER_KEY, PersistentDataType.INTEGER, i + 1);
            pdc.set(GrowthItems.USES_KEY, PersistentDataType.INTEGER, tier.requiredUses());
            item.setItemMeta(meta);
            for (ItemEffect effect : tier.effects())
                effect.apply(item, alpha);
        }

        target.getInventory().addItem(item);

        sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.growth.given_admin",
                growthId, targetLevel, maxLevel, alpha.getPseudo()));

        boolean senderIsTarget = sender instanceof Player sp && sp.getUniqueId().equals(target.getUniqueId());
        if (!senderIsTarget)
            target.sendMessage(ls.text(target, "cmd.growth.given_player",
                    growthId, targetLevel, maxLevel));

        return Command.SINGLE_SUCCESS;
    }

    // ─── lvlup ─────────────────────────────────────────────────────────────────

    /** Cherche un growth item dans la main principale ou secondaire du joueur et fait monter son palier. */
    private static int lvlUpHeld(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer alpha = AlphaPlayerArgument.getAlphaPlayer(ctx, "player");
        Player player = alpha.getPlayer();
        var sender = ctx.getSource().getSender();

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        boolean isOffHand;
        ItemStack heldItem;

        if (GrowthItems.getGrowthId(mainHand) != null) {
            heldItem = mainHand;
            isOffHand = false;
        } else if (GrowthItems.getGrowthId(offHand) != null) {
            heldItem = offHand;
            isOffHand = true;
        } else {
            LangService ls = GameManager.getInstance().getLangService();
            sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.growth.no_held", alpha.getPseudo()));
            return Command.SINGLE_SUCCESS;
        }

        return applyLvlUp(ctx, alpha, player, heldItem, () -> {
            if (isOffHand) player.getInventory().setItemInOffHand(heldItem);
            else player.getInventory().setItemInMainHand(heldItem);
        });
    }

    /** Cherche l'item par ID dans tout l'inventaire (y compris armure) et fait monter son palier. */
    private static int lvlUpById(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer alpha = AlphaPlayerArgument.getAlphaPlayer(ctx, "player");
        String growthId = GrowthItemArgument.getId(ctx, "growthId");
        Player player = alpha.getPlayer();
        var sender = ctx.getSource().getSender();

        ItemWithCommit found = findInFullInventory(player, growthId);
        if (found == null) {
            LangService ls = GameManager.getInstance().getLangService();
            sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.growth.not_in_inventory",
                    alpha.getPseudo(), growthId));
            return Command.SINGLE_SUCCESS;
        }

        return applyLvlUp(ctx, alpha, player, found.item(), found.commit());
    }

    /**
     * Logique commune de lvlup : vérifie le palier max, appelle {@link GrowthItems#forceLvlUp},
     * commit la modification dans l'inventaire, et envoie les feedbacks.
     */
    private static int applyLvlUp(CommandContext<CommandSourceStack> ctx, AlphaPlayer alpha, Player player, ItemStack item, Runnable commit) {
        var sender = ctx.getSource().getSender();
        String growthId = GrowthItems.getGrowthId(item);
        GrowthConfig config = GrowthItemRegistry.get(growthId);

        int currentTier = item.getItemMeta().getPersistentDataContainer().getOrDefault(GrowthItems.TIER_KEY, PersistentDataType.INTEGER, 0);
        if (currentTier >= config.tiers().size()) {
            LangService ls = GameManager.getInstance().getLangService();
            sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.growth.already_maxed",
                    growthId, currentTier, config.tiers().size()));
            return Command.SINGLE_SUCCESS;
        }

        GrowthItems.forceLvlUp(item, alpha);
        commit.run();

        int newTier = item.getItemMeta().getPersistentDataContainer().getOrDefault(GrowthItems.TIER_KEY, PersistentDataType.INTEGER, 0);
        LangService ls = GameManager.getInstance().getLangService();

        sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.growth.lvlup_admin",
                growthId, alpha.getPseudo(), newTier, config.tiers().size()));

        boolean senderIsTarget = sender instanceof Player sp && sp.getUniqueId().equals(player.getUniqueId());
        if (!senderIsTarget)
            player.sendMessage(ls.text(player, "cmd.growth.lvlup_player",
                    growthId, newTier));

        return Command.SINGLE_SUCCESS;
    }

    // ─── info ─────────────────────────────────────────────────────────────────

    private static int showInfo(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer alpha = AlphaPlayerArgument.getAlphaPlayer(ctx, "player");
        var sender = ctx.getSource().getSender();

        List<ItemStack> items = findAllGrowthItems(alpha.getPlayer());

        LangService ls = GameManager.getInstance().getLangService();
        sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.growth.info_header", alpha.getPseudo()));

        if (items.isEmpty()) {
            sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.growth.info_none"));
            return Command.SINGLE_SUCCESS;
        }

        for (ItemStack item : items) {
            String id = GrowthItems.getGrowthId(item);
            GrowthConfig config = GrowthItemRegistry.get(id);
            PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            int tier = pdc.getOrDefault(GrowthItems.TIER_KEY, PersistentDataType.INTEGER, 0);
            int uses = pdc.getOrDefault(GrowthItems.USES_KEY, PersistentDataType.INTEGER, 0);
            int maxTier = config != null ? config.tiers().size() : 0;

            sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.growth.info_entry",
                    id, tier, maxTier, uses));
        }

        return Command.SINGLE_SUCCESS;
    }

    // ─── track ─────────────────────────────────────────────────────────────────

    private static int trackItem(CommandContext<CommandSourceStack> ctx) {
        String growthId = GrowthItemArgument.getId(ctx, "growthId");
        GrowthConfig config = GrowthItemRegistry.get(growthId);
        var sender = ctx.getSource().getSender();

        LangService ls = GameManager.getInstance().getLangService();
        sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.growth.track_header", growthId));

        int found = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            ItemWithCommit result = findInFullInventory(online, growthId);
            if (result == null) continue;

            PersistentDataContainer pdc = result.item().getItemMeta().getPersistentDataContainer();
            int tier = pdc.getOrDefault(GrowthItems.TIER_KEY, PersistentDataType.INTEGER, 0);
            int uses = pdc.getOrDefault(GrowthItems.USES_KEY, PersistentDataType.INTEGER, 0);
            int maxTier = config.tiers().size();

            sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.growth.track_entry",
                    online.getName(), tier, maxTier, uses));
            found++;
        }

        sender.sendMessage(found == 0
                ? ls.text(ls.resolveOrDefault(sender), "cmd.growth.track_none")
                : ls.text(ls.resolveOrDefault(sender), "cmd.growth.track_found", found));

        return Command.SINGLE_SUCCESS;
    }

    // ─── remove ───────────────────────────────────────────────────────────────

    private static int removeItem(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer alpha = AlphaPlayerArgument.getAlphaPlayer(ctx, "player");
        String growthId = GrowthItemArgument.getId(ctx, "growthId");
        Player target = alpha.getPlayer();
        var sender = ctx.getSource().getSender();
        LangService ls = GameManager.getInstance().getLangService();

        int removed = removeAllGrowthItems(target, growthId);

        if (removed == 0) {
            sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.growth.not_in_inventory",
                    alpha.getPseudo(), growthId));
            return Command.SINGLE_SUCCESS;
        }

        sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.growth.removed_admin",
                growthId, removed, alpha.getPseudo()));

        boolean senderIsTarget = sender instanceof Player sp && sp.getUniqueId().equals(target.getUniqueId());
        if (!senderIsTarget)
            target.sendMessage(ls.text(target, "cmd.growth.removed_player", growthId, removed));

        return Command.SINGLE_SUCCESS;
    }

    /** Retire tous les exemplaires de {@code growthId} de tout l'inventaire (principal, offhand, armure). */
    private static int removeAllGrowthItems(Player player, String growthId) {
        PlayerInventory inv = player.getInventory();
        int removed = 0;

        for (int slot = 0; slot < 36; slot++) {
            ItemStack item = inv.getItem(slot);
            if (item != null && !item.getType().isAir() && growthId.equals(GrowthItems.getGrowthId(item))) {
                inv.setItem(slot, null);
                removed++;
            }
        }

        ItemStack offHand = inv.getItemInOffHand();
        if (!offHand.getType().isAir() && growthId.equals(GrowthItems.getGrowthId(offHand))) {
            inv.setItemInOffHand(null);
            removed++;
        }

        ItemStack[] armor = inv.getArmorContents();
        boolean armorChanged = false;
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && !armor[i].getType().isAir() && growthId.equals(GrowthItems.getGrowthId(armor[i]))) {
                armor[i] = null;
                armorChanged = true;
                removed++;
            }
        }
        if (armorChanged) inv.setArmorContents(armor);

        return removed;
    }

    // ─── replace ──────────────────────────────────────────────────────────────

    private static int replaceItem(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer alpha = AlphaPlayerArgument.getAlphaPlayer(ctx, "player");
        String growthId = GrowthItemArgument.getId(ctx, "growthId");
        Player target = alpha.getPlayer();
        var sender = ctx.getSource().getSender();
        LangService ls = GameManager.getInstance().getLangService();

        ECustomItem customItem = ECustomItem.fromString(growthId.toLowerCase());
        if (customItem == null) {
            sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.growth.no_item", growthId));
            return Command.SINGLE_SUCCESS;
        }

        int replaced = replaceAllInInventory(target, growthId, customItem, alpha);

        if (replaced == 0) {
            sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.growth.not_in_inventory",
                    alpha.getPseudo(), growthId));
            return Command.SINGLE_SUCCESS;
        }

        sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.growth.replace_admin",
                growthId, replaced, alpha.getPseudo()));

        boolean senderIsTarget = sender instanceof Player sp && sp.getUniqueId().equals(target.getUniqueId());
        if (!senderIsTarget)
            target.sendMessage(ls.text(target, "cmd.growth.replace_player", growthId, replaced));

        return Command.SINGLE_SUCCESS;
    }

    // ─── replaceall ────────────────────────────────────────────────────────────

    private static int replaceAll(CommandContext<CommandSourceStack> ctx) {
        String growthId = GrowthItemArgument.getId(ctx, "growthId");
        var sender = ctx.getSource().getSender();
        LangService ls = GameManager.getInstance().getLangService();

        ECustomItem customItem = ECustomItem.fromString(growthId.toLowerCase());
        if (customItem == null) {
            sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.growth.no_item", growthId));
            return Command.SINGLE_SUCCESS;
        }

        sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.growth.replaceall_header", growthId));

        int totalReplaced = 0;
        int playersAffected = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            try {
                AlphaPlayer alpha = AlphaPlayer.get(online.getUniqueId());
                int replaced = replaceAllInInventory(online, growthId, customItem, alpha);
                if (replaced > 0) {
                    sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.growth.replaceall_entry",
                            online.getName(), replaced));
                    totalReplaced += replaced;
                    playersAffected++;
                }
            } catch (AlphaPlayerNotFoundException ignored) {}
        }

        sender.sendMessage(ls.text(ls.resolveOrDefault(sender), "cmd.growth.replaceall_done",
                totalReplaced, playersAffected));

        return Command.SINGLE_SUCCESS;
    }

    /**
     * Remplace tous les exemplaires de {@code growthId} dans l'inventaire complet du joueur
     * (main, offhand, armure) par un item fraîchement cloné depuis {@code customItem},
     * en préservant uses, tier, biomes visités et types de mobs tués.
     *
     * @return le nombre d'items effectivement remplacés
     */
    private static int replaceAllInInventory(Player player, String growthId, ECustomItem customItem, AlphaPlayer alpha) {
        PlayerInventory inv = player.getInventory();
        int replaced = 0;

        for (int slot = 0; slot < 36; slot++) {
            ItemStack old = inv.getItem(slot);
            if (old == null || old.getType().isAir()) continue;
            if (!growthId.equals(GrowthItems.getGrowthId(old))) continue;
            inv.setItem(slot, migrateGrowthItem(old, customItem, alpha));
            replaced++;
        }

        ItemStack offHand = inv.getItemInOffHand();
        if (!offHand.getType().isAir() && growthId.equals(GrowthItems.getGrowthId(offHand))) {
            inv.setItemInOffHand(migrateGrowthItem(offHand, customItem, alpha));
            replaced++;
        }

        ItemStack[] armor = inv.getArmorContents();
        boolean armorChanged = false;
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] == null || armor[i].getType().isAir()) continue;
            if (!growthId.equals(GrowthItems.getGrowthId(armor[i]))) continue;
            armor[i] = migrateGrowthItem(armor[i], customItem, alpha);
            armorChanged = true;
            replaced++;
        }
        if (armorChanged) inv.setArmorContents(armor);

        return replaced;
    }

    /**
     * Clone {@code customItem} et transfère les données de progression de l'ancien item :
     * uses, tier, biomes visités ({@link GrowthItems#VISITED_BIOMES_KEY}) et types de mobs
     * tués ({@link GrowthItems#KILLED_MOB_TYPES_KEY}). Appelle ensuite {@link GrowthItems#reapplyAll}
     * pour réappliquer tous les effets persistants du palier atteint sur le nouvel item.
     */
    private static ItemStack migrateGrowthItem(ItemStack old, ECustomItem customItem, AlphaPlayer alpha) {
        PersistentDataContainer oldPdc = old.getItemMeta().getPersistentDataContainer();

        int uses = oldPdc.getOrDefault(GrowthItems.USES_KEY, PersistentDataType.INTEGER, 0);
        int tier = oldPdc.getOrDefault(GrowthItems.TIER_KEY, PersistentDataType.INTEGER, 0);

        ItemStack fresh = customItem.getItemStack().clone();
        ItemMeta freshMeta = fresh.getItemMeta();
        PersistentDataContainer freshPdc = freshMeta.getPersistentDataContainer();

        freshPdc.set(GrowthItems.USES_KEY, PersistentDataType.INTEGER, uses);
        freshPdc.set(GrowthItems.TIER_KEY, PersistentDataType.INTEGER, tier);

        if (oldPdc.has(GrowthItems.VISITED_BIOMES_KEY, PersistentDataType.STRING))
            freshPdc.set(GrowthItems.VISITED_BIOMES_KEY, PersistentDataType.STRING,
                    oldPdc.get(GrowthItems.VISITED_BIOMES_KEY, PersistentDataType.STRING));

        if (oldPdc.has(GrowthItems.KILLED_MOB_TYPES_KEY, PersistentDataType.STRING))
            freshPdc.set(GrowthItems.KILLED_MOB_TYPES_KEY, PersistentDataType.STRING,
                    oldPdc.get(GrowthItems.KILLED_MOB_TYPES_KEY, PersistentDataType.STRING));

        fresh.setItemMeta(freshMeta);
        GrowthItems.reapplyAll(fresh, alpha);

        String growthId = GrowthItems.getGrowthId(fresh);
        GrowthConfig config = GrowthItemRegistry.get(growthId);
        int maxTier = config != null ? config.tiers().size() : -1;
        MLLogManager.getInstance().log(Level.INFO, ELogTag.ITEM,
                "[GrowthItems] Migrate " + alpha.getPseudo() + " : " + growthId
                        + " uses=" + uses + " tier=" + tier + "/" + maxTier);

        return fresh;
    }

    // ─── Utilitaires internes ──────────────────────────────────────────────────

    /**
     * Associe un item (copie extraite de l'inventaire) et le {@link Runnable} qui le remet en place.
     * Nécessaire car les getters d'inventaire (slots, casque…) retournent des copies.
     */
    private record ItemWithCommit(ItemStack item, Runnable commit) {}

    /**
     * Cherche la première occurrence de {@code growthId} dans tout l'inventaire du joueur
     * (main inventaire, main/offhand, armure).
     *
     * @return un {@link ItemWithCommit} ou {@code null} si absent
     */
    @Nullable
    private static ItemWithCommit findInFullInventory(Player player, String growthId) {
        PlayerInventory inv = player.getInventory();

        // Inventaire principal + hotbar (slots 0–35)
        for (int slot = 0; slot < 36; slot++) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType().isAir()) continue;
            if (growthId.equals(GrowthItems.getGrowthId(item))) {
                final int s = slot;
                return new ItemWithCommit(item, () -> inv.setItem(s, item));
            }
        }

        // Main secondaire (slot 40)
        ItemStack offHand = inv.getItemInOffHand();
        if (!offHand.getType().isAir() && growthId.equals(GrowthItems.getGrowthId(offHand)))
            return new ItemWithCommit(offHand, () -> inv.setItemInOffHand(offHand));

        // Armure (slots 36–39 : boots, leggings, chestplate, helmet)
        ItemStack[] armor = inv.getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] == null || armor[i].getType().isAir()) continue;
            if (growthId.equals(GrowthItems.getGrowthId(armor[i]))) {
                final int fi = i;
                return new ItemWithCommit(armor[fi], () -> inv.setArmorContents(armor));
            }
        }

        return null;
    }

    /** Collecte tous les growth items présents dans tout l'inventaire (main, armure, offhand). */
    private static List<ItemStack> findAllGrowthItems(Player player) {
        List<ItemStack> result = new ArrayList<>();
        PlayerInventory inv = player.getInventory();

        for (int slot = 0; slot < 36; slot++) {
            ItemStack item = inv.getItem(slot);
            if (item != null && !item.getType().isAir() && GrowthItems.getGrowthId(item) != null)
                result.add(item);
        }

        ItemStack offHand = inv.getItemInOffHand();
        if (!offHand.getType().isAir() && GrowthItems.getGrowthId(offHand) != null)
            result.add(offHand);

        for (ItemStack armor : inv.getArmorContents()) {
            if (armor != null && !armor.getType().isAir() && GrowthItems.getGrowthId(armor) != null)
                result.add(armor);
        }

        return result;
    }
}