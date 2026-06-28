package fr.miuby.survi.system.command.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.miuby.lib.command.MLStringArgument;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.role.Role;
import fr.miuby.survi.system.command.CommandErrors;

import java.util.Collection;

public class RoleArgument extends MLStringArgument<Role> {

    public static RoleArgument role() {
        return new RoleArgument();
    }

    @Override
    public Role convert(String value) throws CommandSyntaxException {
        Role role = GameManager.getInstance().getRoleLoader().getRole(value);
        if (role == null) throw CommandErrors.ROLE_NOT_FOUND.create(value);
        return role;
    }

    @Override
    protected Collection<String> suggestions() {
        return GameManager.getInstance().getRoleLoader().getRoles()
                .stream().map(r -> r.type().toString()).toList();
    }

    public static Role getRole(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, Role.class);
    }
}
