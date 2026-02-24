package fr.miuby.survi.system.time;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.miuby.survi.GameManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.format.DateTimeFormatter;

public class TimeCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> createCommand() {
        return Commands.literal("time")
            .then(Commands.literal("status")
                .executes(ctx -> {
                    TimeManager tm = GameManager.getInstance().getTimeManager();
                    if (tm == null) {
                        ctx.getSource().getSender().sendMessage(Component.text("TimeManager non initialisé !", NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }
                    var sender = ctx.getSource().getSender();
                    sender.sendMessage(Component.text("Timezone: ", NamedTextColor.YELLOW).append(Component.text(tm.getServerTimezone().getId(), NamedTextColor.WHITE)));
                    sender.sendMessage(Component.text("A reset aujourd'hui: ", NamedTextColor.YELLOW).append(Component.text(tm.hasResetToday() ? "Oui" : "Non", NamedTextColor.WHITE)));
                    sender.sendMessage(Component.text("Dernier reset: jour ", NamedTextColor.YELLOW).append(Component.text(tm.getLastResetDay(), NamedTextColor.WHITE)));
                    sender.sendMessage(Component.text("Prochain reset: ", NamedTextColor.YELLOW).append(Component.text(tm.getNextResetTime().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")), NamedTextColor.WHITE)));
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("forcereset")
                .requires(sender -> sender.getSender().hasPermission("survi.time.admin"))
                .executes(ctx -> {
                    TimeManager tm = GameManager.getInstance().getTimeManager();
                    if (tm == null) {
                        ctx.getSource().getSender().sendMessage(Component.text("TimeManager non initialisé !", NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }
                    ctx.getSource().getSender().sendMessage(Component.text("Force le reset quotidien...", NamedTextColor.GOLD));
                    tm.forceReset();
                    ctx.getSource().getSender().sendMessage(Component.text("✓ Reset effectué avec succès !", NamedTextColor.GREEN));
                    return Command.SINGLE_SUCCESS;
                })
            );
    }
}