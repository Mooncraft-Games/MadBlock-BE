package org.madblock.newgamesapi.commands;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.util.Utility;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.game.HubManager;
import org.madblock.newgamesapi.game.NavigationManager;

import java.util.Optional;

public class CommandHub extends PluginCommand<NewGamesAPI1> {

    public CommandHub() {
        super("hub", NewGamesAPI1.get());
        this.setDescription("Brings up the hub selector.");
        this.setUsage("/hub\nOR /hub <string: hub_type>");

        this.commandParameters.clear();
        this.commandParameters.put("default", new CommandParameter[]{
                CommandParameter.newType("hub_type", true, CommandParamType.STRING)
        });
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {

        if (!sender.isPlayer()) {
            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You must be a player to execute this command.", TextFormat.RED));
            return true;
        }

        Player player = (Player) sender;
        if(args.length < 1) {
            NavigationManager.get().openQuickLobbyMenu(player);
        } else {
            String hubWorldID = args[0].toLowerCase();
            Optional<GameHandler> hub = HubManager.get().getAvailableHub(hubWorldID);
            if(hub.isPresent()){
                hub.get().addPlayerToGame(player);
            } else {
                sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "This hub category doesn't exist.", TextFormat.RED));
            }
        }
        return true;
    }
}
