package org.madblock.newgamesapi.commands;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.game.GameManager;
import org.madblock.ranks.api.RankManager;
import org.madblock.ranks.api.RankProfile;

import java.util.Optional;

public class CommandServer extends PluginCommand<NewGamesAPI1> {

    public CommandServer() {
        super("server", NewGamesAPI1.get());
        this.setDescription("Takes people to sessions.");
        this.setUsage("/server find <String: Player> /server <String: sessionid> \nOR /server");

        this.commandParameters.clear();
        this.commandParameters.put("default", new CommandParameter[0]);
        this.commandParameters.put("goto", new CommandParameter[]{
                CommandParameter.newType("sessionid", CommandParamType.STRING)
        });
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {

        if(args.length == 0){

            if (!sender.isPlayer()) {
                sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You must be a player to execute this command.", TextFormat.RED));
                return true;
            }
            Player player = (Player) sender;

            GameHandler game = GameManager.get().getPlayerLookup().get(player.getUniqueId());

            if(game != null){
                sender.sendMessage(Utility.generateServerMessage("SERVER", TextFormat.DARK_GREEN, String.format("You are in server: %s%s!", TextFormat.GREEN, game.getServerID())));

            } else {
                sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You are not in any server. How????", TextFormat.RED));
            }
            return true;

        } else {

            switch (args[0].toLowerCase()) {

                case "find":

                    if (sender.isPlayer()) {
                        Player player = (Player) sender;

                        Optional<RankProfile> profile = RankManager.getInstance().getRankProfile(player);
                        if ((!player.isOp()) && (!profile.isPresent() || !profile.get().hasPermission("newgameapi.commands.server.find"))) {
                            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You do not have permission to execute this command.", TextFormat.RED));
                            return true;
                        }
                    }

                    if (args.length < 2) {
                        sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Missing 1 parameter (<String: Player>).", TextFormat.RED));
                        return true;
                    }

                    StringBuilder playerName = new StringBuilder();
                    for (int i = 1; i < args.length; i++) playerName.append(args[i]);
                    Player foundPlayer = NewGamesAPI1.get().getServer().getPlayer(playerName.toString());

                    if (foundPlayer == null) {
                        sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "The player specified is not on this node.", TextFormat.RED));
                        return true;
                    }

                    GameHandler game = GameManager.get().getPlayerLookup().get(foundPlayer.getUniqueId());

                    if(game != null){
                        sender.sendMessage(Utility.generateServerMessage("SERVER", TextFormat.DARK_GREEN, String.format("The player is in the server: %s%s!", TextFormat.GREEN, game.getServerID())));

                    } else {
                        sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "This player is not in an NGAPI-powered server", TextFormat.RED));
                    }
                    break;


                default:

                    if (!sender.isPlayer()) {
                        sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You must be a player to execute this command.", TextFormat.RED));
                        return true;
                    }
                    Player player = (Player) sender;
                    String serverID = args[0].toLowerCase();
                    Optional<GameHandler> session = GameManager.get().getSession(serverID);

                    if (session.isPresent()) {

                        if (!session.get().addPlayerToGame(player)) {
                            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Unable to join the target game.", TextFormat.RED));
                        }

                    } else {
                        sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Unable to find the specified server.", TextFormat.RED));
                    }
                break;
            }
        }

        return true;
    }
}
