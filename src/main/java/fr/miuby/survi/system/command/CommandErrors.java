package fr.miuby.survi.system.command;

import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.kyori.adventure.text.Component;

public class CommandErrors {

    public static final DynamicCommandExceptionType PLAYER_NOT_FOUND =
            new DynamicCommandExceptionType(name -> BrigadierHelper.message(
                    Component.text("Joueur introuvable : " + name)
            ));

    public static final DynamicCommandExceptionType VILLAGER_NOT_FOUND =
            new DynamicCommandExceptionType(name -> BrigadierHelper.message(
                    Component.text("Villageois introuvable : " + name)
            ));

    public static final DynamicCommandExceptionType ROLE_NOT_FOUND =
            new DynamicCommandExceptionType(name -> BrigadierHelper.message(
                    Component.text("Role introuvable : " + name)
            ));

    public static final DynamicCommandExceptionType CUSTOM_ITEM_NOT_FOUND =
            new DynamicCommandExceptionType(name -> BrigadierHelper.message(
                    Component.text("Custom item introuvable : " + name)
            ));


    public static final SimpleCommandExceptionType NOT_A_TRADER =
            new SimpleCommandExceptionType(BrigadierHelper.message(
                    Component.text("Ce villageois n'est pas un Trader.")
            ));

    public static final SimpleCommandExceptionType NOT_A_LEVEL_VILLAGER =
            new SimpleCommandExceptionType(BrigadierHelper.message(
                    Component.text("Ce villageois n'est pas un a level.")
            ));

    private CommandErrors() {}
}
