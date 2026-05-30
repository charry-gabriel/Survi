package fr.miuby.survi.system.command;

import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import fr.miuby.lib.command.MLBrigadierHelper;
import net.kyori.adventure.text.Component;

public class CommandErrors {

    public static final DynamicCommandExceptionType PLAYER_NOT_FOUND =
            new DynamicCommandExceptionType(name -> MLBrigadierHelper.message(
                    Component.text("Joueur introuvable : " + name)
            ));

    public static final DynamicCommandExceptionType VILLAGER_NOT_FOUND =
            new DynamicCommandExceptionType(name -> MLBrigadierHelper.message(
                    Component.text("Villageois introuvable : " + name)
            ));

    public static final DynamicCommandExceptionType ROLE_NOT_FOUND =
            new DynamicCommandExceptionType(name -> MLBrigadierHelper.message(
                    Component.text("Role introuvable : " + name)
            ));

    public static final DynamicCommandExceptionType QUEST_NOT_FOUND =
            new DynamicCommandExceptionType(name -> MLBrigadierHelper.message(
                    Component.text("Quête introuvable : " + name)
            ));

    public static final DynamicCommandExceptionType CUSTOM_ITEM_NOT_FOUND =
            new DynamicCommandExceptionType(name -> MLBrigadierHelper.message(
                    Component.text("Objet custom introuvable : " + name)
            ));

    public static final DynamicCommandExceptionType WORLD_NOT_FOUND =
            new DynamicCommandExceptionType(name -> MLBrigadierHelper.message(
                    Component.text("Monde introuvable : " + name)
            ));

    public static final DynamicCommandExceptionType JOB_NOT_FOUND =
            new DynamicCommandExceptionType(name -> MLBrigadierHelper.message(
                    Component.text("Métier introuvable : " + name)
            ));

    public static final SimpleCommandExceptionType NOT_A_LEVEL_VILLAGER =
            new SimpleCommandExceptionType(MLBrigadierHelper.message(
                    Component.text("Ce villageois n'est pas un a level.")
            ));

    private CommandErrors() {}
}
