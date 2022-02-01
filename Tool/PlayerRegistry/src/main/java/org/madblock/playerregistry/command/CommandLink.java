package org.madblock.playerregistry.command;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import org.madblock.playerregistry.PlayerRegistry;

public class CommandLink extends PluginCommand<PlayerRegistry> {

    public CommandLink() {
        super("link", PlayerRegistry.get());
    }

    //TODO: Add an item to the hotbar that allows linking.
    //TODO: !!! Potential way to kill the database if abused, add a cooldown.
    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if(sender instanceof Player) {
            Player player = (Player) sender;
            PlayerRegistry.get().getLinkerFormManager().sendLinkCommandFormTo(player);
        }
        return true;
    }
}
