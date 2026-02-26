package fr.miuby.survi.system.command;

import com.mojang.brigadier.Message;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import net.kyori.adventure.text.Component;

public final class BrigadierHelper {

    public static Message message(Component component) {
        return MessageComponentSerializer.message().serialize(component);
    }
}