package fr.miuby.survi.listener;

import fr.miuby.lib.villager.MLVillager;
import fr.miuby.lib.villager.VillagerLoadedEvent;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.VillagerLevel;
import fr.miuby.survi.villager.VillagerPostLoadActions;
import fr.miuby.survi.villager.blessing.BlessingEffect;
import fr.miuby.survi.villager.event.VillagerLevelUpEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class VillagerListener implements Listener {

    @EventHandler
    public void onVillagerLoaded(VillagerLoadedEvent event) {
        MLVillager villager = event.getVillager();
        VillagerRegistry.register(villager);
        VillagerPostLoadActions.executeAndClear(villager);
    }

    @EventHandler
    public void onVillagerLevelUp(VillagerLevelUpEvent event) {
        VillagerLevel villager = event.getVillagerLevel();
        Bukkit.broadcast(Component.text("<", NamedTextColor.AQUA)
                .append(villager.getDisplayName())
                .append(Component.text("> ", NamedTextColor.AQUA))
                .append(villager.getRecapMessage())
        );

        Sound myCustomSound = Sound.sound(Key.key("ui.toast.challenge_complete"), Sound.Source.AMBIENT, 1f, 1.1f);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(myCustomSound);

            for (BlessingEffect effect : villager.getBlessing().blessingEffects()) {
                effect.applyEffect(villager, AlphaPlayer.get(player.getUniqueId()));
            }
        }
    }
}