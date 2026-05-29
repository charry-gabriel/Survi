package fr.miuby.survi.system.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.quest.GlobalQuest;
import fr.miuby.survi.system.command.CommandErrors;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class GlobalQuestArgument implements CustomArgumentType.Converted<GlobalQuest, String> {

    public static GlobalQuestArgument globalQuest() {
        return new GlobalQuestArgument();
    }

    @Override
    public GlobalQuest convert(String nativeType) throws CommandSyntaxException {
        GlobalQuest quest = GameManager.getInstance().getGlobalQuestManager().getQuest(nativeType);
        if (quest == null) {
            throw CommandErrors.QUEST_NOT_FOUND.create(nativeType);
        }
        return quest;
    }

    @Override
    public @NonNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    @Override
    public <S> @NonNull CompletableFuture<Suggestions> listSuggestions(
            @NonNull CommandContext<S> context, SuggestionsBuilder builder) {

        String remaining = builder.getRemaining().toLowerCase();
        GameManager.getInstance().getGlobalQuestManager().getQuestPool().stream()
                .map(GlobalQuest::getId)
                .filter(id -> id.toLowerCase().startsWith(remaining))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    public static GlobalQuest getGlobalQuest(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, GlobalQuest.class);
    }
}
