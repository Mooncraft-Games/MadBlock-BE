package org.madblock.madbot.command;

import org.madblock.lib.commons.style.Check;

import java.util.HashMap;
import java.util.Optional;

public final class BotCommandRegistry {

    private static BotCommandRegistry primaryInst = null;

    private final HashMap<String, BotCommand> commands;

    public BotCommandRegistry() {
        this.commands = new HashMap<>();
    }


    public Optional<BotCommand> get(String id) {
        Check.notEmptyString(id, "id");
        return Optional.ofNullable(this.commands.get(id));
    }

    public void assign(BotCommand command) {
        Check.nullParam(command, "command");
        Check.notEmptyString(command.getName(), "id");

        this.commands.put(command.getName(), command);
    }

    public void assign(String id, BotCommand command) {
        Check.notEmptyString(id, "id");
        Check.nullParam(command, "command");

        this.commands.put(id, command);
    }


    public String[] getCommands() {
        return this.commands.keySet().toArray(new String[0]);
    }



    public boolean setAsMain() {
        if(BotCommandRegistry.primaryInst == null) {
            BotCommandRegistry.primaryInst = this;
            return true;
        }
        return false;
    }

    public static BotCommandRegistry get() {
        return BotCommandRegistry.primaryInst;
    }
}
