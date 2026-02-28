package fr.miuby.survi.villager.blessing;

import com.onarandombox.MultiverseCore.MultiverseCore;
import com.onarandombox.MultiverseCore.api.MVWorldManager;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.log.LogManager;
import fr.miuby.survi.villager.VillagerLevel;
import fr.miuby.survi.world.EWorld;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class WorldResetEffect extends BlessingEffect {

    private final String mvWorldName;
    private final World.Environment environment;
    private final EWorld safeEWorld;

    public WorldResetEffect(String mvWorldName, World.Environment environment, EWorld safeEWorld) {
        this.mvWorldName = mvWorldName;
        this.environment = environment;
        this.safeEWorld  = safeEWorld;
    }

    @Override
    public void applyEffect(VillagerLevel villager, AlphaPlayer player) {
        MultiverseCore mv = (MultiverseCore) Bukkit.getPluginManager().getPlugin("Multiverse-Core");
        if (mv == null) {
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.WORLD, "Multiverse-Core introuvable !");
            return;
        }

        MVWorldManager wm = mv.getMVWorldManager();

        // 1. Téléporter tous les joueurs qui sont dans ce monde
        World target = Bukkit.getWorld(mvWorldName);
        World safe   = WorldRegistry.get(safeEWorld).getWorld();
        Location safeSpawn = safe.getSpawnLocation();

        if (target != null) {
            for (Player p : target.getPlayers()) {
                p.teleport(safeSpawn);
                p.sendMessage("§6[Monde] §eLe monde §b" + mvWorldName + " §eest en cours de reset. Vous avez été téléporté.");
            }
        }

        // 2. Supprimer le monde
        if (!wm.deleteWorld(mvWorldName)) {
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.WORLD,
                    "Impossible de supprimer le monde " + mvWorldName + " via Multiverse !");
            return;
        }

        // 3. Recréer le monde
        boolean created = wm.addWorld(mvWorldName, environment, null, WorldType.NORMAL, true, null);
        if (!created) {
            LogManager.getInstance().log(Level.SEVERE, LogManager.ETagLog.WORLD,
                    "Impossible de recréer le monde " + mvWorldName + " via Multiverse !");
            return;
        }

        Bukkit.broadcastMessage("§6[Événement] §eLe monde §b" + mvWorldName + " §ea été réinitialisé !");
        LogManager.getInstance().log(Level.INFO, LogManager.ETagLog.WORLD, "Monde " + mvWorldName + " resetté avec succès.");
    }
}