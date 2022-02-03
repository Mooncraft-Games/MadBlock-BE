package org.madblock.madbot.link;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.madblock.madbot.MadBot;
import org.madblock.madbot.command.account.CommandLink;
import org.madblock.madbot.text.Emoji;
import org.madblock.playerregistry.link.IntegrationLinker;
import org.madblock.playerregistry.link.KnownLinkSources;
import org.madblock.util.DatabaseReturn;

import java.util.HashMap;

public class MadBotLinker {

    protected HashMap<String, String> playerCodes;

    public MadBotLinker() {
        this.playerCodes = new HashMap<>();
    }

    public void handleAppButtonPushed(ButtonInteractionEvent e) {
        switch (e.getCustomId()) {
            case CommandLink.ENTER_CODE:
                break;

            case CommandLink.GEN_CODE:
                EmbedCreateSpec specGeneratingCode = EmbedCreateSpec.builder()
                        .title("Account Linking "+ Emoji.LINK_GOLD.get().asFormat())
                        .description("Generating a pair code, please wait.")
                        .color(Color.ORANGE)
                        .build();


                e.edit()
                        .withEmbeds(specGeneratingCode)
                        .withComponents()
                        .block();

                MadBot.get().getServer().getScheduler().scheduleTask(MadBot.get(), () -> {
                    DatabaseReturn<String> dbreturn = IntegrationLinker.linkFromPlatform(KnownLinkSources.DISCORD, e.getInteraction().getUser().getId().asString());

                    if(dbreturn.isPresent()) {
                        StringBuilder str = new StringBuilder();

                        for(char c: dbreturn.get().toCharArray())
                            str.append(Emoji.codeToEmojiFormatted(c)).append(" ");


                        EmbedCreateSpec specAccountLink = EmbedCreateSpec.builder()
                                .title("Account Linking "+ Emoji.LINK_GOLD.get().asFormat())
                                .description("Here's your code! Check the hidden message.")
                                .color(Color.CYAN)
                                .build();
                        e.editFollowup(e.getMessageId()).withEmbeds(specAccountLink).block();

                        e.createFollowup(str.toString()).withEphemeral(true).block();

                    } else {

                        //TODO: Detailed error message
                        EmbedCreateSpec specFail = EmbedCreateSpec.builder()
                                .title("Account Linking "+ Emoji.LINK_GOLD.get().asFormat())
                                .description("Uh oh! Something went wrong. Try again later.")
                                .color(Color.RED)
                                .build();
                        e.createFollowup().withEmbeds(specFail).block();
                    }


                }, true);

                break;
        }
    }

}
