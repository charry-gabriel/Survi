package fr.miuby.survi.player.service;

import fr.miuby.lib.villager.MLVillager;
import fr.miuby.lib.villager.VillagerRegistry;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.player.AlphaPlayer;
import fr.miuby.survi.villager.villagerlevel.VillagerLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Réapplique à la connexion tous les effets de blessings actifs (toutes sources confondues),
 * puis envoie un récapitulatif de l'état actif au joueur.
 *
 * Ce service n'est pas lié exclusivement aux villageois — il a vocation à centraliser
 * toute réapplication d'effets persistants qui disparaissent à la déconnexion.
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

        Component globalText = recap.build();
        if (PlainTextComponentSerializer.plainText().serialize(globalText).isBlank()) return;

        player.getPlayer().sendMessage(Component.text()
                .append(Component.text("-------------------- Récap --------------------", NamedTextColor.YELLOW)).appendNewline()
                .append(Component.text("Difficulté Niv. " + GameManager.getInstance().getWorldLevelManager().getLevel(), NamedTextColor.YELLOW)).appendNewline()
                .append(globalText)
                .append(Component.text("----------------------------------------------", NamedTextColor.YELLOW))
                .build());
    }
}