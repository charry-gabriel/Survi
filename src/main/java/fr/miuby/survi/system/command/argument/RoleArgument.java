package fr.miuby.survi.system.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.role.Role;
import fr.miuby.survi.system.command.CommandErrors;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class RoleArgument implements CustomArgumentType.Converted<Role, String> {

    public static RoleArgument role() {
        return new RoleArgument();
    }

    @Override
    public Role convert(String nativeType) throws CommandSyntaxException {
        Role role = GameManager.getInstance().getRoleLoader().getRole(nativeType);

        if (role == null) {
            throw CommandErrors.ROLE_NOT_FOUND.create(nativeType);
        }

        return role;
    }

    @Override
    public @NonNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    @Override
    public <S> @NonNull CompletableFuture<Suggestions> listSuggestions(@NonNull CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();

        GameManager.getInstance().getRoleLoader().getRoles().stream().map(r -> r.type().toString()).filter(name -> name.toLowerCase().startsWith(remaining)).forEach(builder::suggest);

        return builder.buildFuture();
    }

    public static Role getRole(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, Role.class);
    }
}