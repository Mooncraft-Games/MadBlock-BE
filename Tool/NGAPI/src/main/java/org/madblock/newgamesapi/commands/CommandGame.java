package org.madblock.newgamesapi.commands;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.command.data.CommandEnum;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.util.Utility;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.game.GameManager;
import org.madblock.newgamesapi.registry.GameRegistry;
import org.madblock.ranks.api.RankManager;
import org.madblock.ranks.api.RankProfile;

import java.util.Arrays;
import java.util.Optional;

public class CommandGame extends PluginCommand<NewGamesAPI1> {

    protected static CommandGame commandGame;

    public CommandGame() {
        super("game", NewGamesAPI1.get());
        commandGame = this;
        this.setDescription("Handles games");
        this.setUsage("/game start <String: gameid> \nOR /game sentto <String: sessionid> \nOR /game stop <String: sessionid>");

        this.commandParameters.clear();
        this.commandParameters.put("start", new CommandParameter[]{
                CommandParameter.newEnum("start", new CommandEnum("StartGame","start")),
                CommandParameter.newEnum("gameid", GameRegistry.get().getGames().toArray(new String[GameRegistry.get().getGames().size()]))
        });
        this.commandParameters.put("sendlobbytogame", new CommandParameter[]{
                CommandParameter.newEnum("sendto", new CommandEnum("SendToGame","sendto")),
                CommandParameter.newType("sessionid", CommandParamType.STRING)
        });
        this.commandParameters.put("stop", new CommandParameter[]{
                CommandParameter.newEnum("stop", new CommandEnum("StopGame","stop")),
                CommandParameter.newType("sessionid", CommandParamType.STRING)
        });
    }

    public static void refreshParameters() {
        commandGame.commandParameters.put("start", new CommandParameter[]{
                CommandParameter.newEnum("start", new CommandEnum("StartGame","start")),
                CommandParameter.newEnum("gameid", GameRegistry.get().getGames().toArray(new String[GameRegistry.get().getGames().size()]))
        });
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (sender.isPlayer()) {
            Optional<RankProfile> profile = RankManager.getInstance().getRankProfile((Player)sender);
            if (!profile.isPresent() || !profile.get().hasPermission("newgameapi.commands.game")) {
                sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You do not have permission to execute this command.", TextFormat.RED));
                return true;
            }
        }

        sender.sendMessage(Utility.generateServerMessage("DEBUG", TextFormat.GOLD, "Args: "+ Arrays.toString(args), TextFormat.YELLOW));
        if(args.length < 2){
            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, String.format("Invalid parameters (At least 2 params expected. Found %s): %s", args.length, getUsage()), TextFormat.RED));
            return true;
        }

        switch (args[0].toLowerCase()){
            case "start":
                try {
                    sender.sendMessage(Utility.generateServerMessage("GAME", TextFormat.DARK_PURPLE, "Creating game..."));
                    String game = GameManager.get().createGameSession(args[1], 120);
                    sender.sendMessage(Utility.generateServerMessage("GAME", TextFormat.DARK_PURPLE, "Created game with session id: "+game));
                } catch (Exception err){
                    sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An error was encountered whilst creating a game:\n"+err.getMessage(), TextFormat.RED));
                }
                return true;
            case "stop":
                Optional<GameHandler> sgh = GameManager.get().getSession(args[1]);
                if(sgh.isPresent()){
                    if(sgh.get().endGame(true, true)){
                        sender.sendMessage(Utility.generateServerMessage("GAME", TextFormat.DARK_PURPLE, String.format("Stopped game successfully with id: %s", args[1])));
                    } else {
                        sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, String.format("Game Session with id [%s] failed to stop.", args[1]), TextFormat.RED));
                    }
                } else {
                    sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, String.format("Game Session with id [%s] does not exist", args[1]), TextFormat.RED));
                }
                return true;
            case "sendto":
                if(sender instanceof Player) {
                    Optional<GameHandler> sltggh = GameManager.get().getSession(args[1]);
                    if (sltggh.isPresent()) {
                        sender.sendMessage(Utility.generateServerMessage("GAME", TextFormat.DARK_PURPLE, "Sending players to "+args[1]));
                        sltggh.get().prepare(((Player) sender).getLevel().getPlayers().values().toArray(new Player[0]));
                    } else {
                        sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, String.format("Game Session with id [%s] does not exist", args[1]), TextFormat.RED));
                    }
                } else {
                    sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You must be a player to run that command.", TextFormat.RED));
                }
                return true;
            default:
                sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Invalid parameters (1st Param Invalid): "+getUsage(), TextFormat.RED));
                return true;
        }
    }
}
