package fr.miuby.survi.grave;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.miuby.lib.log.MLLogManager;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.command.argument.AlphaPlayerArgument;
import fr.miuby.survi.system.lang.ELang;
import fr.miuby.survi.system.lang.LangService;
import fr.miuby.survi.system.log.ELogTag;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class GraveCommand {

    private GraveCommand() {}

    /**
     * Sous-arbre admin à brancher sur /survi : /survi grave recover <joueur>
     * Récupère la tombe d'un joueur et lui transfère les items (nécessite que le joueur soit connecté).
     */
    public static LiteralArgumentBuilder<CommandSourceStack> createAdminSubTree() {
        return Commands.literal("grave")
                .then(Commands.literal("recover")
                        .then(Commands.argument("joueur", AlphaPlayerArgument.alphaPlayer())
                                .executes(ctx -> {
                                    AlphaPlayer target = AlphaPlayerArgument.getAlphaPlayer(ctx, "joueur");
                                    CommandSender sender = ctx.getSource().getSender();
                                    LangService ls = GameManager.getInstance().getLangService();
                                    ELang lang = ls.resolveOrDefault(sender);

                                    Player targetPlayer = target.getPlayer();
                                    if (targetPlayer == null) {
                                        sender.sendMessage(ls.text(lang, "cmd.grave.player_offline", target.getPseudo()));
                                        MLLogManager.getInstance().log(Level.WARNING, ELogTag.GRAVE,
                                                "[GraveCmd/Admin] Impossible de récupérer la tombe de " + target.getPseudo() + " : joueur hors ligne");
                                        return Command.SINGLE_SUCCESS;
                                    }

                                    boolean found = GameManager.getInstance().getGraveManager().collectGraveByOwner(targetPlayer, target.getUuid());
                                    if (found) {
                                        sender.sendMessage(ls.text(lang, "cmd.grave.recovered_admin", target.getPseudo()));
                                        targetPlayer.sendMessage(ls.text(targetPlayer, "grave.recovered"));
                                        MLLogManager.getInstance().log(Level.INFO, ELogTag.GRAVE,
                                                "[GraveCmd/Admin] " + sender.getName() + " a récupéré la tombe de " + target.getPseudo());
                                    } else {
                                        sender.sendMessage(ls.text(lang, "cmd.grave.no_grave_admin", target.getPseudo()));
                                        MLLogManager.getInstance().log(Level.FINE, ELogTag.GRAVE,
                                                "[GraveCmd/Admin] " + sender.getName() + " → aucune tombe active pour " + target.getPseudo());
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                );
    }
}