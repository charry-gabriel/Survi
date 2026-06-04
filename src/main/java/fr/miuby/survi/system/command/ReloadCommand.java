package fr.miuby.survi.system.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.miuby.lib.resource.MLResourceManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.growth_item.GrowthItemLoader;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Sous-arbre {@code /survi reload} — rechargement à chaud des fichiers YAML.
 *
 * <h3>Usage</h3>
 * <pre>
 *   /survi reload                  → recharge tous les YAML supportés
 *   /survi reload quests           → quests.yml uniquement
 *   /survi reload global_quests    → global_quests.yml uniquement
 *   /survi reload monsters         → monsters.yml uniquement
 *   /survi reload roles            → roles.yml uniquement
 *   /survi reload growth_items     → growth_items/*.yml uniquement
 *   /survi reload villagers        → invalide le cache villagers/*.yml + traders/*.yml
 * </pre>
 *
 * <h3>Non-rechargeables (redémarrage requis)</h3>
 * <ul>
 *   <li>{@code recipes.yml} — Bukkit ne permet pas de dé-enregistrer des recettes à chaud.</li>
 *   <li>{@code config.yml} — trop de sous-systèmes live en dépendent (TimeManager, VillageZoneManager…).</li>
 * </ul>
 */
@SuppressWarnings({"java:S3516", "SameReturnValue"})
public class ReloadCommand {

    private ReloadCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("reload")
                .requires(source -> source.getSender().isOp())
                .executes(ReloadCommand::reloadAll)
                .then(Commands.literal("quests").executes(ReloadCommand::reloadQuests))
                .then(Commands.literal("global_quests").executes(ReloadCommand::reloadGlobalQuests))
                .then(Commands.literal("monsters").executes(ReloadCommand::reloadMonsters))
                .then(Commands.literal("roles").executes(ReloadCommand::reloadRoles))
                .then(Commands.literal("growth_items").executes(ReloadCommand::reloadGrowthItems))
                .then(Commands.literal("villagers").executes(ReloadCommand::reloadVillagers));
    }

    // =========================================================================
    // Tout recharger
    // =========================================================================

    private static int reloadAll(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        sender.sendMessage(Component.text("Rechargement global en cours...").color(NamedTextColor.GRAY));

        // quests.yml + global_quests.yml (lecture directe YamlConfiguration)
        int quests = GameManager.getInstance().getQuestManager().reload();
        int globalQuests = GameManager.getInstance().getGlobalQuestManager().reload();

        // monsters.yml (lecture directe YamlConfiguration, sans relancer le timer)
        GameManager.getInstance().getMobLevelManager().reload();

        // roles.yml (lecture directe YamlConfiguration, clear registre inclus)
        GameManager.getInstance().getRoleLoader().reload();

        // growth_items/*.yml via MLResourceManager — clearCache() inclus, donc
        // les caches villagers/*.yml et traders/*.yml sont aussi invalidés.
        GrowthItemLoader.reload();

        sender.sendMessage(Component.text("✔ Rechargement terminé :").color(NamedTextColor.GREEN));
        sender.sendMessage(Component.text("  quests.yml → ").color(NamedTextColor.GRAY).append(Component.text(quests + " quête(s)", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  global_quests.yml → ").color(NamedTextColor.GRAY).append(Component.text(globalQuests + " quête(s) globale(s)", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  monsters.yml ✔ — roles.yml ✔ — growth_items/*.yml ✔").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  villagers/*.yml + traders/*.yml → rechargés au prochain accès").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  ⚠ recipes.yml et config.yml nécessitent un redémarrage.").color(NamedTextColor.YELLOW));
        return Command.SINGLE_SUCCESS;
    }

    // =========================================================================
    // Recharges ciblées
    // =========================================================================

    private static int reloadQuests(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        sender.sendMessage(Component.text("Rechargement de quests.yml...").color(NamedTextColor.GRAY));
        int loaded = GameManager.getInstance().getQuestManager().reload();
        sender.sendMessage(Component.text("✔ ").color(NamedTextColor.GREEN).append(Component.text(loaded + " quête(s) rechargée(s). Voir la console pour les éventuelles quêtes orphelines.", NamedTextColor.WHITE)));
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadGlobalQuests(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        sender.sendMessage(Component.text("Rechargement de global_quests.yml...").color(NamedTextColor.GRAY));
        int loaded = GameManager.getInstance().getGlobalQuestManager().reload();
        sender.sendMessage(Component.text("✔ ").color(NamedTextColor.GREEN).append(Component.text(loaded + " quête(s) globale(s) rechargée(s). La quête active en cours n'est pas interrompue.", NamedTextColor.WHITE)));
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadMonsters(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        sender.sendMessage(Component.text("Rechargement de monsters.yml...").color(NamedTextColor.GRAY));
        GameManager.getInstance().getMobLevelManager().reload();
        sender.sendMessage(Component.text("✔ monsters.yml rechargé. Les mobs déjà spawnés conservent leurs stats actuelles.").color(NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadRoles(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        sender.sendMessage(Component.text("Rechargement de roles.yml...").color(NamedTextColor.GRAY));
        GameManager.getInstance().getRoleLoader().reload();
        sender.sendMessage(Component.text("✔ roles.yml rechargé. Les attributs des joueurs connectés restent inchangés jusqu'à reconnexion.").color(NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadGrowthItems(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        sender.sendMessage(Component.text("Rechargement de growth_items/*.yml...").color(NamedTextColor.GRAY));
        GrowthItemLoader.reload();
        sender.sendMessage(Component.text("✔ growth_items/*.yml rechargés.").color(NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadVillagers(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        sender.sendMessage(Component.text("Invalidation du cache villagers/*.yml + traders/*.yml...").color(NamedTextColor.GRAY));
        MLResourceManager.clearCache();
        sender.sendMessage(Component.text("✔ Cache invalidé — les configs seront rechargées depuis le disque au prochain accès.").color(NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }
}