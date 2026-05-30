package fr.miuby.survi.system.command.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.miuby.lib.command.MLStringArgument;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.role.Role;
import fr.miuby.survi.system.command.CommandErrors;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SubRoleArgument extends MLStringArgument<Role> {

    public static SubRoleArgument subrole() {
        return new SubRoleArgument();
    }

    @Override
    public Role convert(String value) throws CommandSyntaxException {
        Role role = GameManager.getInstance().getRoleLoader().getRole(value);
        if (role == null) throw CommandErrors.ROLE_NOT_FOUND.create(value);
        return role;
    }

    /**
     * Les suggestions dépendent du contexte (joueur cible + action add/remove),
     * donc on surcharge directement {@link #listSuggestions} plutôt que {@link #suggestions}.
     */
    @Override
    public <S> @NonNull CompletableFuture<Suggestions> listSuggestions(
            @NonNull CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        AlphaPlayer alphaPlayer = AlphaPlayerArgument.getAlphaPlayer(context, "player");
        boolean isAdd = context.getInput().contains("add");

        getSubRoleSuggestions(alphaPlayer, isAdd).stream()
                .filter(name -> name.toLowerCase().startsWith(remaining))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    private static List<String> getSubRoleSuggestions(AlphaPlayer alphaPlayer, boolean isAdd) {
        List<String> roles = new ArrayList<>();
        if (isAdd) {
            roles.addAll(GameManager.getInstance().getRoleLoader().getRoles()
                    .stream().map(r -> r.type().toString()).toList());
            roles.removeAll(alphaPlayer.getSubRoles().stream()
                    .map(r -> r.type().toString()).toList());
            if (alphaPlayer.getRole() != null) {
                roles.remove(alphaPlayer.getRole().type().toString());
            }
        } else {
            roles.addAll(alphaPlayer.getSubRoles().stream()
                    .map(r -> r.type().toString()).toList());
        }
        return roles;
    }

    public static Role getSubRole(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, Role.class);
    }
}
