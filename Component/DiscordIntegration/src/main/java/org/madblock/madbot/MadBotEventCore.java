package org.madblock.madbot;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.madblock.madbot.command.BotCommand;
import org.madblock.madbot.command.BotCommandRegistry;

import java.util.Optional;

public class MadBotEventCore {

    public static void handleAppCommandExecuted(ApplicationCommandInteractionEvent e) {
        Optional<BotCommand> command = BotCommandRegistry.get().get(e.getCommandName());

        if(command.isPresent()) {
            command.get().execute(e).block();
            return;
        }

        EmbedCreateSpec failSpec = EmbedCreateSpec.builder()
                .title("Error")
                .description("Command unrecognized. Let someone know as this is unintended. <3")
                .color(Color.RED)
                .build();

        e.reply().withEmbeds(failSpec).block();
    }

    public static void handleAppButtonPushed(ButtonInteractionEvent e) {

    }

}
