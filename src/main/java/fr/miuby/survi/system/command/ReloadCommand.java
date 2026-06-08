package fr.miuby.survi.system.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
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
 *   /survi reload quests           → quests/*.yml uniquement
 *   /survi reload global_quests    → global_quests.yml uniquement
 *   /survi reload monsters         → monsters.yml uniquement
 *   /survi reload roles            → roles.yml + ré-application immédiate sur les joueurs connectés
 *   /survi reload growth_items     → growth_items/*.yml uniquement
 *   /survi reload villagers        → villagers/*.yml + traders/*.yml (entités en jeu mises à jour)
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

        int quests = GameManager.getInstance().getQuestManager().reload();
        int globalQuests = GameManager.getInstance().getGlobalQuestManager().reload();
        GameManager.getInstance().getMobLevelManager().reload();
        GameManager.getInstance().getRoleLoader().reload();           // clear + reload + ré-application joueurs
        GrowthItemLoader.reload();                                     // clear registre + clearCache + reload
        GameManager.getInstance().getVillagerFactory().reloadAll();   // clearCache + reload VillagerLevel + Traders

        sender.sendMessage(Component.text("✔ Rechargement terminé :").color(NamedTextColor.GREEN));
        sender.sendMessage(Component.text("  quests/*.yml → ").color(NamedTextColor.GRAY).append(Component.text(quests + " quête(s)", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  global_quests.yml → ").color(NamedTextColor.GRAY).append(Component.text(globalQuests + " quête(s) globale(s)", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  monsters.yml ✔ — roles.yml ✔ (joueurs connectés mis à jour) — growth_items/*.yml ✔").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  villagers/*.yml + traders/*.yml ✔ (entités en jeu mises à jour, inventaires recalculés)").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  ⚠ recipes.yml et config.yml nécessitent un redémarrage.").color(NamedTextColor.YELLOW));
        return Command.SINGLE_SUCCESS;
    }

    // =========================================================================
    // Recharges ciblées
    // =========================================================================

    private static int reloadQuests(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        sender.sendMessage(Component.text("Rechargement de quests/*.yml...").color(NamedTextColor.GRAY));
        int loaded = GameManager.getInstance().getQuestManager().reload();
        sender.sendMessage(Component.text("✔ " + loaded + " quête(s). Quêtes orphelines traitées (voir console).").color(NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadGlobalQuests(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        sender.sendMessage(Component.text("Rechargement de global_quests.yml...").color(NamedTextColor.GRAY));
        int loaded = GameManager.getInstance().getGlobalQuestManager().reload();
        sender.sendMessage(Component.text("✔ " + loaded + " quête(s) globale(s). La quête active en cours n'est pas interrompue.").color(NamedTextColor.GREEN));
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
        sender.sendMessage(Component.text("✔ roles.yml rechargé. Attributs ré-appliqués immédiatement sur les joueurs connectés.").color(NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadGrowthItems(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        sender.sendMessage(Component.text("Rechargement de growth_items/*.yml...").color(NamedTextColor.GRAY));
        GrowthItemLoader.reload();
        sender.sendMessage(Component.text("✔ growth_items/*.yml rechargés. Les items des joueurs fonctionnent immédiatement avec la nouvelle config.").color(NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadVillagers(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        sender.sendMessage(Component.text("Rechargement de villagers/*.yml + traders/*.yml...").color(NamedTextColor.GRAY));
        GameManager.getInstance().getVillagerFactory().reloadAll();
        sender.sendMessage(Component.text("✔ Villageois et traders mis à jour en jeu. Inventaires tribute recalculés (items déjà donnés pris en compte).").color(NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }
}