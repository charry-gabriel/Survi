package fr.miuby.survi.item.growth_item;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.miuby.survi.item.ECustomItem;
import fr.miuby.survi.item.growth_item.config.GrowthConfig;
import fr.miuby.survi.item.growth_item.effect.ItemEffect;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.command.argument.AlphaPlayerArgument;
import fr.miuby.survi.system.command.argument.GrowthItemArgument;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

/**
 * Commandes d'administration pour les growth items.
 *
 * <pre>
 * /growthitem give   {@code <player> <growthId> [level]} — donne l'item au palier indiqué (défaut : 0, item frais)
 * /growthitem lvlup  {@code <player> [growthId]}         — force le palier suivant (item en main si sans growthId)
 * /growthitem info   {@code <player>}                    — liste tous les growth items dans l'inventaire du joueur
 * /growthitem track  {@code <growthId>}                  — joueurs en ligne possédant cet item (inventaire inclus armure)
 * /growthitem remove {@code <player> <growthId>}         — retire tous les exemplaires de l'item de l'inventaire
 * </pre>
 *
 * <p>⚠ {@code track} et {@code remove} ne fonctionnent que sur les joueurs <b>en ligne</b> :
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

        if (targetLevel > maxLevel) {
            sender.sendMessage(Component.text("Niveau trop élevé. Max pour " + growthId + " : " + maxLevel + ".", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        ECustomItem customItem = ECustomItem.fromString(growthId.toLowerCase());
        if (customItem == null) {
            sender.sendMessage(Component.text("Aucune entrée ECustomItem pour l'ID : " + growthId + ".", NamedTextColor.RED));
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

        sender.sendMessage(
                Component.text("✓ ", NamedTextColor.GREEN)
                        .append(Component.text(growthId, NamedTextColor.GOLD))
                        .append(Component.text(" palier " + targetLevel + "/" + maxLevel + " donné à ", NamedTextColor.GREEN))
                        .append(Component.text(alpha.getPseudo(), NamedTextColor.YELLOW)));

        boolean senderIsTarget = sender instanceof Player senderPlayer && senderPlayer.getUniqueId().equals(target.getUniqueId());
        if (!senderIsTarget)
            target.sendMessage(
                    Component.text("Vous avez reçu ", NamedTextColor.GREEN)
                            .append(Component.text(growthId, NamedTextColor.GOLD))
                            .append(Component.text(" (palier " + targetLevel + "/" + maxLevel + ").", NamedTextColor.GREEN)));

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
            sender.sendMessage(Component.text(alpha.getPseudo() + " ne tient pas de growth item.", NamedTextColor.RED));
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
            sender.sendMessage(
                    Component.text(alpha.getPseudo() + " n'a pas ", NamedTextColor.RED)
                            .append(Component.text(growthId, NamedTextColor.GOLD))
                            .append(Component.text(" dans son inventaire.", NamedTextColor.RED)));
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
            sender.sendMessage(
                    Component.text(growthId, NamedTextColor.GOLD)
                            .append(Component.text(" est déjà au palier maximum (" + currentTier + "/" + config.tiers().size() + ").", NamedTextColor.RED)));
            return Command.SINGLE_SUCCESS;
        }

        GrowthItems.forceLvlUp(item, alpha);
        commit.run();

        int newTier = item.getItemMeta().getPersistentDataContainer().getOrDefault(GrowthItems.TIER_KEY, PersistentDataType.INTEGER, 0);

        sender.sendMessage(
                Component.text("✓ ", NamedTextColor.GREEN)
                        .append(Component.text(growthId, NamedTextColor.GOLD))
                        .append(Component.text(" de " + alpha.getPseudo() + " : palier ", NamedTextColor.GREEN))
                        .append(Component.text(newTier + "/" + config.tiers().size(), NamedTextColor.WHITE)));

        boolean senderIsTarget = sender instanceof Player senderPlayer && senderPlayer.getUniqueId().equals(player.getUniqueId());
        if (!senderIsTarget)
            player.sendMessage(
                    Component.text("Votre ", NamedTextColor.GREEN)
                            .append(Component.text(growthId, NamedTextColor.GOLD))
                            .append(Component.text(" est passé au palier " + newTier + " !", NamedTextColor.GREEN)));

        return Command.SINGLE_SUCCESS;
    }

    // ─── info ─────────────────────────────────────────────────────────────────

    private static int showInfo(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer alpha = AlphaPlayerArgument.getAlphaPlayer(ctx, "player");
        var sender = ctx.getSource().getSender();

        List<ItemStack> items = findAllGrowthItems(alpha.getPlayer());

        sender.sendMessage(Component.text("══ Growth items de ", NamedTextColor.GOLD)
                .append(Component.text(alpha.getPseudo(), NamedTextColor.YELLOW))
                .append(Component.text(" ══", NamedTextColor.GOLD)));

        if (items.isEmpty()) {
            sender.sendMessage(Component.text("  Aucun.", NamedTextColor.GRAY));
            return Command.SINGLE_SUCCESS;
        }

        for (ItemStack item : items) {
            String id = GrowthItems.getGrowthId(item);
            GrowthConfig config = GrowthItemRegistry.get(id);
            PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            int tier = pdc.getOrDefault(GrowthItems.TIER_KEY, PersistentDataType.INTEGER, 0);
            int uses = pdc.getOrDefault(GrowthItems.USES_KEY, PersistentDataType.INTEGER, 0);
            int maxTier = config != null ? config.tiers().size() : 0;
            boolean maxed = tier >= maxTier;

            sender.sendMessage(
                    Component.text("  » ", NamedTextColor.GRAY)
                            .append(Component.text(id, NamedTextColor.GOLD))
                            .append(Component.text(" — palier ", NamedTextColor.YELLOW))
                            .append(Component.text(tier + "/" + maxTier, maxed ? NamedTextColor.GREEN : NamedTextColor.WHITE))
                            .append(Component.text(" — " + uses + " uses", NamedTextColor.GRAY)));
        }

        return Command.SINGLE_SUCCESS;
    }

    // ─── track ─────────────────────────────────────────────────────────────────

    private static int trackItem(CommandContext<CommandSourceStack> ctx) {
        String growthId = GrowthItemArgument.getId(ctx, "growthId");
        GrowthConfig config = GrowthItemRegistry.get(growthId);
        var sender = ctx.getSource().getSender();

        sender.sendMessage(Component.text("══ Joueurs avec ", NamedTextColor.AQUA)
                .append(Component.text(growthId, NamedTextColor.GOLD))
                .append(Component.text(" (en ligne uniquement) ══", NamedTextColor.AQUA)));

        int found = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            ItemWithCommit result = findInFullInventory(online, growthId);
            if (result == null) continue;

            PersistentDataContainer pdc = result.item().getItemMeta().getPersistentDataContainer();
            int tier = pdc.getOrDefault(GrowthItems.TIER_KEY, PersistentDataType.INTEGER, 0);
            int uses = pdc.getOrDefault(GrowthItems.USES_KEY, PersistentDataType.INTEGER, 0);
            int maxTier = config.tiers().size();
            boolean maxed = tier >= maxTier;

            sender.sendMessage(
                    Component.text("  » ", NamedTextColor.GRAY)
                            .append(Component.text(online.getName(), NamedTextColor.YELLOW))
                            .append(Component.text(" — palier ", NamedTextColor.WHITE))
                            .append(Component.text(tier + "/" + maxTier, maxed ? NamedTextColor.GREEN : NamedTextColor.WHITE))
                            .append(Component.text(" — " + uses + " uses", NamedTextColor.GRAY)));
            found++;
        }

        Component summary = found == 0
                ? Component.text("  Aucun joueur en ligne ne possède cet item.", NamedTextColor.GRAY)
                : Component.text("  " + found + " joueur(s) trouvé(s).", NamedTextColor.GRAY);
        sender.sendMessage(summary);

        return Command.SINGLE_SUCCESS;
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