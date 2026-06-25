package fr.miuby.survi.blessing;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import fr.miuby.survi.world.EWorld;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.logging.Level;

public class FlyEffect extends BlessingEffect {

    @Override
    public void applyEffect(AlphaPlayer player) {
        Player p = player.getPlayer();
        if (!isInVillage(p)) {
            MLLogManager.getInstance().log(Level.FINE, ELogTag.VILLAGER,
                    "[FlyEffect] " + player.getPseudo() + " hors du Village — vol différé au changement de monde");
            return;
        }
        enableFly(p);
    }

    @Override
    public void resetEffect(AlphaPlayer player) {
        Player p = player.getPlayer();
        if (p == null) return;
        disableFly(p);
        MLLogManager.getInstance().log(Level.INFO, ELogTag.VILLAGER,
                "[FlyEffect] reset — vol retiré pour " + player.getPseudo());
    }

    @Override
    public boolean requiresOnlinePlayer() { return true; }

    // ─── Helpers statiques appelés aussi depuis PlayerListener ───────────────────

    public static void enableFly(Player player) {
        player.setAllowFlight(true);
        player.setFlying(true);
        MLLogManager.getInstance().log(Level.INFO, ELogTag.VILLAGER,
                "[FlyEffect] vol activé pour " + player.getName());
    }

    public static void disableFly(Player player) {
        player.setFlying(false);
        player.setAllowFlight(false);
        MLLogManager.getInstance().log(Level.FINE, ELogTag.VILLAGER,
                "[FlyEffect] vol désactivé (hors Village) pour " + player.getName());
    }

    /** Retourne {@code true} si le blessing FlyEffect est actif sur au moins un VillagerLevel. */
    public static boolean isActive() {
        return VillagerRegistry.getAll().stream()
                .filter(v -> v instanceof VillagerLevel)
                .map(v -> (VillagerLevel) v)
                .flatMap(vl -> Arrays.stream(vl.getCurrentBlessings()))
                .flatMap(b -> Arrays.stream(b.blessingEffects()))
                .anyMatch(e -> e instanceof FlyEffect);
    }

    public static boolean isInVillage(Player player) {
        var mlWorld = WorldRegistry.get(player.getWorld().getUID());
        return mlWorld != null && mlWorld.getType() == EWorld.VILLAGE;
    }
}