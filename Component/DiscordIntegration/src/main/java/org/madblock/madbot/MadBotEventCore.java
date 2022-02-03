package org.madblock.madbot;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import org.madblock.madbot.command.BotCommand;
import org.madblock.madbot.command.BotCommandRegistry;

import java.util.HashMap;
import java.util.Optional;

public class MadBotEventCore {


    public static void handleAppCommandExecuted(ApplicationCommandInteractionEvent e) {
        Optional<BotCommand> command = BotCommandRegistry.get().get(e.getCommandName());

        if(command.isPresent()) {
            command.get().execute(e).block();
            return;
        }
    }

    public static void handleAppButtonPushed(ButtonInteractionEvent e) {

    }

}
