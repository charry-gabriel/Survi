package fr.miuby.survi.system.command.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.miuby.lib.command.MLStringArgument;
import fr.miuby.survi.job.EJob;
import fr.miuby.survi.system.command.CommandErrors;

import java.util.Arrays;
import java.util.Collection;

/**
 * Argument Brigadier pour sélectionner un {@link EJob} par son nom (insensible à la casse).
 *
 * <pre>
 *   Commands.argument("job", JobArgument.job())
 *   EJob job = JobArgument.getJob(ctx, "job");
 * </pre>
 */
public class JobArgument extends MLStringArgument<EJob> {

    public static JobArgument job() {
        return new JobArgument();
    }

    @Override
    public EJob convert(String value) throws CommandSyntaxException {
        try {
            return EJob.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException _) {
            throw CommandErrors.JOB_NOT_FOUND.create(value);
        }
    }

    @Override
    protected Collection<String> suggestions() {
        return Arrays.stream(EJob.values()).map(j -> j.name().toLowerCase()).toList();
    }

    public static EJob getJob(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, EJob.class);
    }
}
