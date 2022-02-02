package org.madblock.madbot.command;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import org.madblock.lib.commons.style.Check;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public abstract class BotCommand {

    private final String name;
    private final String description;
    private final ArrayList<ApplicationCommandOptionData> parameters;

    public BotCommand() {
        String tmpDesc = this.getCommandDescription();
        ArrayList<ApplicationCommandOptionData> tmpParams = this.getCommandParameters();

        this.name = Check.notEmptyString(this.getCommandName(), "getCommandName()");
        this.description = tmpDesc == null ? "" : tmpDesc;
        this.parameters = tmpParams == null ? new ArrayList<>() : tmpParams;
    }

    protected abstract String getCommandName();
    protected abstract String getCommandDescription();
    protected abstract ArrayList<ApplicationCommandOptionData> getCommandParameters();

    public abstract Mono<Void> execute(ApplicationCommandInteractionEvent source);


    public final String getName() {
        return this.name;
    }

    public final String getDescription() {
        return this.description;
    }

    public final ArrayList<ApplicationCommandOptionData> getParameters() {
        return new ArrayList<>(this.parameters);
    }
}
