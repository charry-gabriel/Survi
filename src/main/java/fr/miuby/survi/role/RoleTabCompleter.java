package fr.miuby.survi.role;

import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RoleTabCompleter implements TabCompleter {
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("role")) {
            if (args.length == 1) {
                return StringUtil.copyPartialMatches(args[0],
                        GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayers().values().stream().map(AlphaPlayer::getPseudo).toList(),
                        new ArrayList<>());
            } else if (args.length == 2) {
                return StringUtil.copyPartialMatches(args[1],
                        GameManager.getInstance().getRoleFactory().getRoles().stream().map(role -> role.type().toString()).toList(),
                        new ArrayList<>());
            }
        }
        return null;
    }
}
