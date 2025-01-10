package fr.miuby.survi.role;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SubRoleTabCompleter implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("subrole")) {
            if (args.length == 1) {
                List<String> subCommands = new ArrayList<>();
                subCommands.add("add");
                subCommands.add("remove");

                return StringUtil.copyPartialMatches(args[0],
                        subCommands,
                        new ArrayList<>());
            }
            else if (args.length == 2) {
                return StringUtil.copyPartialMatches(args[1],
                        GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers().values().stream().map(AlphaPlayer::getPseudo).toList(),
                        new ArrayList<>());
            } else if (args.length == 3) {
                AlphaPlayer alphaPlayer = GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayer(args[1]);

                List<String> roles = new ArrayList<>();
                if (args[0].equals("add")) {
                    roles.addAll(GameManager.getInstance().getRoleFactory().getRoles().stream().map(role -> role.type().toString()).toList());
                    roles.removeAll(alphaPlayer.getSubRoles().stream().map(role -> role.type().toString()).toList());
                    roles.remove(alphaPlayer.getRole().type().toString());
                }
                else if (args[0].equals("remove")) {
                    roles.addAll(alphaPlayer.getSubRoles().stream().map(role -> role.type().toString()).toList());
                }
                return StringUtil.copyPartialMatches(args[2],
                        roles,
                        new ArrayList<>());
            }
        }
        return null;
    }
}
