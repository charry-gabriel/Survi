package fr.miuby.survi.system.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.item.growth_item.GrowthItemLoader;
import fr.miuby.survi.item.growth_item.GrowthItems;
import fr.miuby.survi.job.config.JobsLoader;
import fr.miuby.survi.system.lang.ELang;
import fr.miuby.survi.system.lang.LangService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
                .then(Commands.literal("villagers").executes(ReloadCommand::reloadVillagers))
                .then(Commands.literal("jobs").executes(ReloadCommand::reloadJobs))
                .then(Commands.literal("recipes").executes(ReloadCommand::reloadRecipes))
                .then(Commands.literal("zone").executes(ReloadCommand::reloadZone))
                .then(Commands.literal("lang").executes(ReloadCommand::reloadLang))
                .then(Commands.literal("rare_items").executes(ReloadCommand::reloadRareItems));
    }

    private static int reloadAll(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        LangService   ls     = ls();
        ELang         lang   = lang(sender);

        sender.sendMessage(ls.text(lang, "cmd.reload.all.start"));

        int quests       = GameManager.getInstance().getQuestManager().reload();
        int globalQuests = GameManager.getInstance().getGlobalQuestManager().reload();
        GameManager.getInstance().getMobLevelManager().reload();
        GameManager.getInstance().getRoleLoader().reload();
        GrowthItemLoader.reload();
        reapplyGrowthItemsForOnlinePlayers();
        GameManager.getInstance().getVillagerFactory().reloadAll();
        JobsLoader.reload();
        GameManager.getInstance().getCustomRecipeFactory().reload();
        GameManager.getInstance().getVillageZoneManager().reloadConfig();
        GameManager.getInstance().getLangService().reload();
        GameManager.getInstance().getRareItemService().reload();

        sender.sendMessage(ls.text(lang, "cmd.reload.all.done", quests, globalQuests));
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadQuests(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        LangService   ls     = ls();
        ELang         lang   = lang(sender);
        sender.sendMessage(ls.text(lang, "cmd.reload.quests.start"));
        int loaded = GameManager.getInstance().getQuestManager().reload();
        sender.sendMessage(ls.text(lang, "cmd.reload.quests.done", loaded));
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadGlobalQuests(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        LangService   ls     = ls();
        ELang         lang   = lang(sender);
        sender.sendMessage(ls.text(lang, "cmd.reload.global_quests.start"));
        int loaded = GameManager.getInstance().getGlobalQuestManager().reload();
        sender.sendMessage(ls.text(lang, "cmd.reload.global_quests.done", loaded));
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadMonsters(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        LangService   ls     = ls();
        ELang         lang   = lang(sender);
        sender.sendMessage(ls.text(lang, "cmd.reload.monsters.start"));
        GameManager.getInstance().getMobLevelManager().reload();
        sender.sendMessage(ls.text(lang, "cmd.reload.monsters.done"));
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadRoles(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        LangService   ls     = ls();
        ELang         lang   = lang(sender);
        sender.sendMessage(ls.text(lang, "cmd.reload.roles.start"));
        GameManager.getInstance().getRoleLoader().reload();
        sender.sendMessage(ls.text(lang, "cmd.reload.roles.done"));
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadGrowthItems(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        LangService   ls     = ls();
        ELang         lang   = lang(sender);
        sender.sendMessage(ls.text(lang, "cmd.reload.growth_items.start"));
        GrowthItemLoader.reload();
        reapplyGrowthItemsForOnlinePlayers();
        sender.sendMessage(ls.text(lang, "cmd.reload.growth_items.done"));
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadVillagers(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        LangService   ls     = ls();
        ELang         lang   = lang(sender);
        sender.sendMessage(ls.text(lang, "cmd.reload.villagers.start"));
        GameManager.getInstance().getVillagerFactory().reloadAll();
        sender.sendMessage(ls.text(lang, "cmd.reload.villagers.done"));
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadJobs(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        LangService   ls     = ls();
        ELang         lang   = lang(sender);
        sender.sendMessage(ls.text(lang, "cmd.reload.jobs.start"));
        JobsLoader.reload();
        sender.sendMessage(ls.text(lang, "cmd.reload.jobs.done"));
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadRecipes(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        LangService   ls     = ls();
        ELang         lang   = lang(sender);
        sender.sendMessage(ls.text(lang, "cmd.reload.recipes.start"));
        GameManager.getInstance().getCustomRecipeFactory().reload();
        sender.sendMessage(ls.text(lang, "cmd.reload.recipes.done"));
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadZone(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        LangService   ls     = ls();
        ELang         lang   = lang(sender);
        sender.sendMessage(ls.text(lang, "cmd.reload.zone.start"));
        GameManager.getInstance().getVillageZoneManager().reloadConfig();
        sender.sendMessage(ls.text(lang, "cmd.reload.zone.done"));
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadLang(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        LangService   ls     = ls();
        ELang         lang   = lang(sender);
        sender.sendMessage(ls.text(lang, "cmd.reload.lang.start"));
        GameManager.getInstance().getLangService().reload();
        sender.sendMessage(ls.text(lang, "cmd.reload.lang.done"));
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadRareItems(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        LangService   ls     = ls();
        ELang         lang   = lang(sender);
        sender.sendMessage(ls.text(lang, "cmd.reload.rare_items.start"));
        GameManager.getInstance().getRareItemService().reload();
        sender.sendMessage(ls.text(lang, "cmd.reload.rare_items.done"));
        return Command.SINGLE_SUCCESS;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Force la réapplication immédiate des growth items (nom, attributs, enchantements)
     * sur tout ce que les joueurs actuellement connectés tiennent ou portent.
     *
     * <p>Sans cela, un joueur en ligne au moment du reload ne voit le changement que s'il
     * touche son équipement (équiper/déséquiper, swap main/offhand) ou déclenche l'event
     * métier de l'item (miner, pêcher…) — cf. {@code GrowthItemListener}. Les joueurs
     * déconnectés au moment du reload sont eux couverts à la reconnexion ({@code onPlayerJoin}).</p>
     */
    private static void reapplyGrowthItemsForOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers())
            GrowthItems.checkAndReapplyHeldAndEquipped(player);
    }

    private static LangService ls() {
        return GameManager.getInstance().getLangService();
    }

    private static ELang lang(CommandSender sender) {
        LangService ls = ls();
        return sender instanceof Player p ? ls.resolveLanguage(p) : ls.getServerDefault();
    }
}