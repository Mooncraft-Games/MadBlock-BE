package org.madblock.madbot.command.account;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.util.Color;
import org.madblock.madbot.command.BotCommand;
import org.madblock.madbot.text.Emoji;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

public class CommandLink extends BotCommand {

    public static final String GEN_CODE = "linker_gen_code";
    public static final String ENTER_CODE = "linker_use_code";


    @Override
    protected String getCommandName() {
        return "link";
    }

    @Override
    protected String getCommandDescription() {
        return "Links a user with their in-game account";
    }

    @Override
    protected ArrayList<ApplicationCommandOptionData> getCommandParameters() {
        return new ArrayList<>();
    }

    @Override
    public Mono<Void> execute(ApplicationCommandInteractionEvent source) {

        EmbedCreateSpec embedCreateSpec = EmbedCreateSpec.builder()
                .title("Account Linking "+ Emoji.LINK_GOLD.get().asFormat())
                .description("Welcome to the account linker! Please select one of the following buttons to get started.")
                .color(Color.CYAN)
                .build();

        return source.reply()
                .withEmbeds(embedCreateSpec)
                .withComponents(ActionRow.of(
                        Button.primary(GEN_CODE, "Get New Code"),
                        Button.primary(ENTER_CODE, "Enter Code")
                ));
    }
}
