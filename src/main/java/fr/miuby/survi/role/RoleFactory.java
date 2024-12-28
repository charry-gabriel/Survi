package fr.miuby.survi.role;

import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RoleFactory {
    private final Map<ERole, Role> roles = new HashMap<>();

    public RoleFactory() {
        roles.put(ERole.VOYAGEUR, new Role("Voyageur", ERole.VOYAGEUR, NamedTextColor.GRAY));
        roles.put(ERole.CAPITAINE, new Role("Capitaine", ERole.CAPITAINE, NamedTextColor.DARK_RED));
        roles.put(ERole.PILOTE, new Role("Pilote", ERole.PILOTE, NamedTextColor.BLUE));
        roles.put(ERole.TEST, new Role("Test", ERole.TEST, NamedTextColor.BLACK));
    }

    public Role getRole(ERole role) {
        return roles.get(role);
    }

    public Collection<Role> getRoles() {
        return roles.values();
    }
}
