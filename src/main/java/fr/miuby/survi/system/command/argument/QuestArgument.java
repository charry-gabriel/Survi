package fr.miuby.survi.system.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.miuby.survi.quest.Quest;
import fr.miuby.survi.quest.QuestManager;
import fr.miuby.survi.system.command.CommandErrors;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

public class QuestArgument implements CustomArgumentType.Converted<Quest, String> {

    public static QuestArgument quest() {
        return new QuestArgument();
    }

    @Override
    public Quest convert(String nativeType) throws CommandSyntaxException {
        Quest quest = QuestManager.getInstance().getQuest(nativeType);

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
    public <S> @NonNull CompletableFuture<Suggestions> listSuggestions(@NonNull CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();

        QuestManager.getInstance().getQuestPool().stream()
                .map(Quest::getId)
                .filter(id -> id.toLowerCase().startsWith(remaining))
                .forEach(builder::suggest);

        return builder.buildFuture();
    }

    public static Quest getQuest(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, Quest.class);
    }
}
