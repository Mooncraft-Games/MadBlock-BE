package org.madblock.newgamesapi.commands;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.util.Utility;
import org.madblock.newgamesapi.game.QuiccccQueueManager;

public class CommandMatchmake extends PluginCommand<NewGamesAPI1> {

    public CommandMatchmake() {
        super("matchmake", NewGamesAPI1.get());
        this.setDescription("Enters people onto the matchmaker.");
        this.setUsage("/matchmake <String: game_type>");

        this.commandParameters.clear();
        this.commandParameters.put("default", new CommandParameter[]{
                CommandParameter.newType("game_type", CommandParamType.STRING)
        });
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {

        if (!sender.isPlayer()) {
            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You must be a player to execute this command.", TextFormat.RED));
            return true;
        }

        Player player = (Player) sender;

        if(args.length < 1){
            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Missing 1 parameter (<String: game_type>).", TextFormat.RED));
            return true;
        } else {
            QuiccccQueueManager.get().matchmakePlayer(player, args[0].toLowerCase());
        }

        return true;
    }
}
