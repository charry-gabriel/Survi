package fr.miuby.survi.system.command;

import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import fr.miuby.lib.command.MLBrigadierHelper;

public class CommandErrors {

    public static final DynamicCommandExceptionType PLAYER_NOT_FOUND       = MLBrigadierHelper.notFound("Joueur");
    public static final DynamicCommandExceptionType VILLAGER_NOT_FOUND     = MLBrigadierHelper.notFound("Villageois");
    public static final DynamicCommandExceptionType ROLE_NOT_FOUND         = MLBrigadierHelper.notFound("Role");
    public static final DynamicCommandExceptionType QUEST_NOT_FOUND        = MLBrigadierHelper.notFound("Quête");
    public static final DynamicCommandExceptionType CUSTOM_ITEM_NOT_FOUND  = MLBrigadierHelper.notFound("Objet custom");
    public static final DynamicCommandExceptionType WORLD_NOT_FOUND        = MLBrigadierHelper.notFound("Monde");
    public static final DynamicCommandExceptionType JOB_NOT_FOUND          = MLBrigadierHelper.notFound("Métier");
    public static final SimpleCommandExceptionType  NOT_A_LEVEL_VILLAGER   = MLBrigadierHelper.simpleError("Ce villageois n'est pas un a level.");

    private CommandErrors() {}
}