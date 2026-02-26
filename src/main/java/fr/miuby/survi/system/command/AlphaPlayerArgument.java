package fr.miuby.survi.system.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class AlphaPlayerArgument implements CustomArgumentType.Converted<AlphaPlayer, String> {

    public static AlphaPlayerArgument alphaPlayer() {
        return new AlphaPlayerArgument();
    }

    @Override
    public AlphaPlayer convert(String nativeType) throws CommandSyntaxException {
        AlphaPlayer alphaPlayer = GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayer(nativeType);

        if (alphaPlayer == null) {
            throw CommandErrors.PLAYER_NOT_FOUND.create(nativeType);
        }

        return alphaPlayer;
    }

    @Override
    public @NonNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    @Override
    public <S> @NonNull CompletableFuture<Suggestions> listSuggestions(@NonNull CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();

        GameManager.getInstance().getAlphaPlayerFactory().getAllPseudo().stream().filter(name -> name.toLowerCase().startsWith(remaining)).forEach(builder::suggest);

        return builder.buildFuture();
    }

    public static AlphaPlayer getAlphaPlayer(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, AlphaPlayer.class);
    }
}