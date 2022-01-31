package org.madblock.playerregistry.command;

import cn.nukkit.command.PluginCommand;
import org.madblock.playerregistry.PlayerRegistry;

public class CommandLink extends PluginCommand<PlayerRegistry> {

    public CommandLink() {
        super("link", PlayerRegistry.get());
    }

}
