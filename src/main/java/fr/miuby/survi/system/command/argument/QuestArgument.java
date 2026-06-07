package fr.miuby.survi.system.command.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.miuby.lib.command.MLStringArgument;
import fr.miuby.survi.GameManager;
import fr.miuby.survi.quest.quest.Quest;
import fr.miuby.survi.system.command.CommandErrors;

import java.util.Collection;

public class QuestArgument extends MLStringArgument<Quest> {

    public static QuestArgument quest() {
        return new QuestArgument();
    }

    @Override
    public Quest convert(String value) throws CommandSyntaxException {
        Quest quest = GameManager.getInstance().getQuestManager().getQuest(value);
        if (quest == null) throw CommandErrors.QUEST_NOT_FOUND.create(value);
        return quest;
    }

    @Override
    protected Collection<String> suggestions() {
        return GameManager.getInstance().getQuestManager().getQuestPool()
                .stream().map(Quest::getId).toList();
    }

    public static Quest getQuest(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, Quest.class);
    }
}
