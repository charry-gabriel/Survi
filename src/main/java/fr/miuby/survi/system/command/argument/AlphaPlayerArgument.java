package fr.miuby.survi.system.command.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.miuby.lib.command.MLStringArgument;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.command.CommandErrors;

import java.util.Collection;

public class AlphaPlayerArgument extends MLStringArgument<AlphaPlayer> {

    public static AlphaPlayerArgument alphaPlayer() {
        return new AlphaPlayerArgument();
    }

    @Override
    public AlphaPlayer convert(String value) throws CommandSyntaxException {
        AlphaPlayer player = GameManager.getInstance().getAlphaPlayerFactory().getAlphaPlayer(value);
        if (player == null) throw CommandErrors.PLAYER_NOT_FOUND.create(value);
        return player;
    }

    @Override
    protected Collection<String> suggestions() {
        return GameManager.getInstance().getAlphaPlayerFactory().getAllPseudo();
    }

    public static AlphaPlayer getAlphaPlayer(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, AlphaPlayer.class);
    }
}
