package fr.miuby.survi.player.service;

import fr.miuby.lib.villager.MLVillager;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.system.lang.LangService;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Réapplique à la connexion tous les effets de blessings actifs (toutes sources confondues),
 * puis envoie un récapitulatif de l'état actif au joueur.
 */
public class PlayerEffectRestoreService {

    public void restoreOnJoin(AlphaPlayer player) {
        TextComponent.Builder recap = Component.text();

        for (MLVillager villager : VillagerRegistry.getAll()) {
            if (!(villager instanceof VillagerLevel villagerLevel)) continue;
            villagerLevel.applyAllCurrentBlessing(player);
            TextComponent recapMessage = villagerLevel.getRecapMessage();
            if (recapMessage != null && !PlainTextComponentSerializer.plainText().serialize(recapMessage).isBlank()) {
                recap.append(recapMessage).append(Component.newline());
            }
        }

        GameManager.getInstance().getLockedItemsFactory().applyLockState(player);

        Component globalText = recap.build();
        if (PlainTextComponentSerializer.plainText().serialize(globalText).isBlank()) return;

        LangService ls = GameManager.getInstance().getLangService();
        int worldLevel = GameManager.getInstance().getWorldLevelManager().getLevel();

        player.getPlayer().sendMessage(Component.text()
                .append(ls.text(player.getPlayer(), "effect.restore.header")).appendNewline()
                .append(ls.text(player.getPlayer(), "effect.restore.world_level", worldLevel)).appendNewline()
                .append(globalText)
                .append(ls.text(player.getPlayer(), "effect.restore.footer"))
                .build());
    }
}