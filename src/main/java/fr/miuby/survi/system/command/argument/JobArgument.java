package fr.miuby.survi.system.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.system.command.CommandErrors;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * Argument Brigadier pour sélectionner un {@link EJob} par son nom (insensible à la casse).
 *
 * Utilisation en commande :
 * <pre>
 *   Commands.argument("job", JobArgument.job())
 *   EJob job = JobArgument.getJob(ctx, "job");
 * </pre>
 *
 * L'auto-complétion propose les noms de tous les métiers en minuscules.
 */
public class JobArgument implements CustomArgumentType.Converted<EJob, String> {

    private JobArgument() {}

    public static JobArgument job() {
        return new JobArgument();
    }

    @Override
    public @NonNull EJob convert(String value) throws CommandSyntaxException {
        try {
            return EJob.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException _) {
            throw CommandErrors.JOB_NOT_FOUND.create(value);
        }
    }

    @Override
    public @NonNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    @Override
    public <S> @NonNull CompletableFuture<Suggestions> listSuggestions(
            @NonNull CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        Arrays.stream(EJob.values())
                .map(j -> j.name().toLowerCase())
                .filter(name -> name.startsWith(remaining))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    public static EJob getJob(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, EJob.class);
    }
}
