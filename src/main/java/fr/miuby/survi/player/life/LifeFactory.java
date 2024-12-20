package fr.miuby.survi.player.life;

import fr.miuby.survi.role.ERole;
import fr.miuby.survi.world.EWorld;

import java.util.ArrayList;
import java.util.List;

public class LifeFactory {
    private final List<LifeModifier> lifeModifiers = new ArrayList<>();
    public LifeFactory() {
        this.lifeModifiers.add(new LifeModifier(EWorld.VILLAGE, ERole.MAIRE, 1.5f));
        this.lifeModifiers.add(new LifeModifier(EWorld.WILDERNESS, ERole.MAIRE, 0.75f));
        this.lifeModifiers.add(new LifeModifier(EWorld.END, null, 0.5f));
    }

    public float getLifeModifier(EWorld world, ERole role) {
        for (LifeModifier lifeModifier : lifeModifiers) {
            if (world == lifeModifier.getWorld() && (lifeModifier.getRole() == null || role == lifeModifier.getRole())) {
                return lifeModifier.getLifeModifier();
            }
        }
        return 1f;
    }
}
