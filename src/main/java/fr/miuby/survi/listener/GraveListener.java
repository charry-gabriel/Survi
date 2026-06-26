package fr.miuby.survi.listener;

import fr.miuby.lib.log.MLLogManager;
import fr.miuby.lib.world.MLWorld;
import fr.miuby.lib.world.WorldRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.grave.GraveData;
import fr.miuby.survi.system.database.repository.GraveLostNotificationRepository;
import fr.miuby.survi.system.lang.LangService;
import fr.miuby.survi.system.log.ELogTag;
import fr.miuby.survi.world.EWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.logging.Level;

public class GraveListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        LangService ls = GameManager.getInstance().getLangService();

        // Tombes actives
        List<GraveData> graves = GameManager.getInstance().getGraveManager().getGravesForPlayer(player.getUniqueId());
        if (!graves.isEmpty()) {
            player.sendMessage(ls.text(player, "grave.join_header",
                    Placeholder.unparsed("count", String.valueOf(graves.size()))));
            for (GraveData grave : graves) {
                Location loc = grave.location();
                MLWorld w = WorldRegistry.get(loc.getWorld().getUID());
                player.sendMessage(ls.text(player, "grave.join_entry",
                        Placeholder.unparsed("x", Integer.toString(loc.getBlockX())),
                        Placeholder.unparsed("y", Integer.toString(loc.getBlockY())),
                        Placeholder.unparsed("z", Integer.toString(loc.getBlockZ())),
                        Placeholder.component("world", Component.text(w.getName(), w.getColor()))));
            }
            MLLogManager.getInstance().log(Level.FINE, ELogTag.GRAVE,
                    "[JoinNotify] " + player.getName() + " → " + graves.size() + " tombe(s) active(s) notifiée(s)");
        }

        // Tombes perdues suite à un reset de monde (joueur était hors ligne)
        List<GraveLostNotificationRepository.LostGraveEntry> lostEntries =
                GameManager.getInstance().getDatabase().graveLostNotifications().loadForPlayer(player.getUniqueId());
        if (!lostEntries.isEmpty()) {
            for (GraveLostNotificationRepository.LostGraveEntry entry : lostEntries) {
                player.sendMessage(ls.text(player, "grave.lost_world_reset",
                        Placeholder.unparsed("x", Integer.toString(entry.x())),
                        Placeholder.unparsed("y", Integer.toString(entry.y())),
                        Placeholder.unparsed("z", Integer.toString(entry.z()))));
            }
            GameManager.getInstance().getDatabase().graveLostNotifications().deleteForPlayer(player.getUniqueId());
            MLLogManager.getInstance().log(Level.FINE, ELogTag.GRAVE,
                    "[JoinNotify] " + player.getName() + " → " + lostEntries.size() + " tombe(s) perdue(s) notifiée(s)");
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (WorldRegistry.get(event.getPlayer().getWorld().getUID()).getType() == EWorld.VILLAGE)
            return;

        boolean created = GameManager.getInstance().getGraveManager().createGrave(event.getEntity());
        if (created) {
            event.getDrops().clear();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null)
            return;

        if (GameManager.getInstance().getGraveManager().collectGrave(event.getPlayer(), event.getClickedBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (GameManager.getInstance().getGraveManager().isGrave(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(GameManager.getInstance().getLangService().text(event.getPlayer(), "grave.indestructible"));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> GameManager.getInstance().getGraveManager().isGrave(block.getLocation()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> GameManager.getInstance().getGraveManager().isGrave(block.getLocation()));
    }
}