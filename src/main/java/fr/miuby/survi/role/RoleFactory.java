package fr.miuby.survi.role;

import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RoleFactory {
    private final Map<ERole, Role> roles = new HashMap<>();

    public RoleFactory() {
        roles.put(ERole.SIMPLET, new Role("Simplet", ERole.SIMPLET, NamedTextColor.GRAY));
        roles.put(ERole.MAIRE, new Role("Maire", ERole.MAIRE, NamedTextColor.RED));
        roles.put(ERole.COUPLE, new Role("Couple", ERole.COUPLE, NamedTextColor.LIGHT_PURPLE));
        roles.put(ERole.JUMP, new Role("Jump", ERole.JUMP, NamedTextColor.BLACK));
        //roles.put(ERole.RICHE, new Role("Riche", ERole.RICHE));
    }

    public Role getRole(ERole role) {
        return roles.get(role);
    }

    public Collection<Role> getRoles() {
        return roles.values();
    }
}
