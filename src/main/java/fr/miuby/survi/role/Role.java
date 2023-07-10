package fr.miuby.survi.role;

public class Role {
    protected final String name;
    protected final ERole type;

    public Role(String name, ERole type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public ERole getType() {
        return type;
    }
}
