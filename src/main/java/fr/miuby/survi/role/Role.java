package fr.miuby.survi.role;

import fr.miuby.survi.GameManager;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class Role {
    private final String name;
    private final ERole type;
    private final NamedTextColor color;

    public Role(String name, ERole type, NamedTextColor color) {
        this.name = name;
        this.type = type;
        this.color = color;
    }

    public static Role get(ERole roleType) {
        return GameManager.getInstance().getRoleFactory().getRole(roleType);
    }

    public static Role get(String roleName) {
        for (Role role : GameManager.getInstance().getRoleFactory().getRoles()) {
            if (role.getName().equals(roleName)) {
                return role;
            }
        }
        throw new NullPointerException(roleName + " role not found !");
    }

    public String getName() {
        return name;
    }

    public ERole getType() {
        return type;
    }

    public TextColor getColor() {
        return color;
    }
}
