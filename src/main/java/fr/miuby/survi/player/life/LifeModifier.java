package fr.miuby.survi.player.life;

import fr.miuby.survi.role.ERole;
import fr.miuby.survi.world.EWorld;

public class LifeModifier {
    private final EWorld world;
    private final ERole role;
    private final float lifeModifier;

    public LifeModifier(EWorld world, ERole role, float lifeModifier) {
        this.world = world;
        this.role = role;
        this.lifeModifier = lifeModifier;
    }

    public float getLifeModifier() {
        return lifeModifier;
    }

    public ERole getRole() {
        return role;
    }

    public EWorld getWorld() {
        return world;
    }
}
