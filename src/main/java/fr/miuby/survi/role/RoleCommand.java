package fr.miuby.survi.role;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.command.argument.AlphaPlayerArgument;
import fr.miuby.survi.system.command.argument.RoleArgument;
import fr.miuby.survi.system.command.argument.SubRoleArgument;
import fr.miuby.survi.system.lang.ELang;
import fr.miuby.survi.system.lang.LangService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RoleCommand {
    private static final String playerArgument  = "player";
    private static final String roleArgument    = "role";
    private static final String subRoleArgument = "subrole";

    private RoleCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> createRoleCommand() {
        return Commands.literal("role")
                .requires(source -> source.getSender().isOp())
                .then(Commands.argument(playerArgument, AlphaPlayerArgument.alphaPlayer())
                        .then(Commands.argument(roleArgument, RoleArgument.role())
                                .executes(RoleCommand::executeRole)
                        )
                );
    }

    public static LiteralArgumentBuilder<CommandSourceStack> createSubRoleCommand() {
        return Commands.literal("subrole")
                .requires(source -> source.getSender().isOp())
                .then(Commands.literal("add")
                        .then(Commands.argument(playerArgument, AlphaPlayerArgument.alphaPlayer())
                                .then(Commands.argument(subRoleArgument, SubRoleArgument.subrole())
                                        .executes(ctx -> executeSubRole(ctx, true))
                                )
                        )
                )
                .then(Commands.literal("remove")
                        .then(Commands.argument(playerArgument, AlphaPlayerArgument.alphaPlayer())
                                .then(Commands.argument(subRoleArgument, SubRoleArgument.subrole())
                                        .executes(ctx -> executeSubRole(ctx, false))
                                )
                        )
                );
    }

    private static int executeRole(CommandContext<CommandSourceStack> ctx) {
        AlphaPlayer alphaPlayer = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);
        Role        role        = RoleArgument.getRole(ctx, roleArgument);

        if (GameManager.getInstance().getRoleManagementService().changeMainRole(alphaPlayer, role)) {
            LangService    ls   = GameManager.getInstance().getLangService();
            CommandSender  sender = ctx.getSource().getSender();
            ELang          lang = sender instanceof Player p ? ls.resolveLanguage(p) : ls.getServerDefault();
            sender.sendMessage(ls.text(lang, "cmd.role.assigned",
                    Placeholder.component("role", role.displayName()),
                    Placeholder.unparsed("player", alphaPlayer.getPseudo())));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int executeSubRole(CommandContext<CommandSourceStack> ctx, boolean isAdd) {
        AlphaPlayer alphaPlayer = AlphaPlayerArgument.getAlphaPlayer(ctx, playerArgument);
        Role        role        = SubRoleArgument.getSubRole(ctx, subRoleArgument);

        RoleManagementService service = GameManager.getInstance().getRoleManagementService();
        boolean actionDone = isAdd ? service.addSubRole(alphaPlayer, role) : service.removeSubRole(alphaPlayer, role);

        if (actionDone) {
            LangService   ls   = GameManager.getInstance().getLangService();
            CommandSender sender = ctx.getSource().getSender();
            ELang         lang = sender instanceof Player p ? ls.resolveLanguage(p) : ls.getServerDefault();
            String        key  = isAdd ? "cmd.subrole.added" : "cmd.subrole.removed";
            sender.sendMessage(ls.text(lang, key,
                    Placeholder.component("role", role.displayName()),
                    Placeholder.unparsed("player", alphaPlayer.getPseudo())));
        }

        return Command.SINGLE_SUCCESS;
    }
}