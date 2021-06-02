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
import org.madblock.newgamesapi.game.GameID;
import org.madblock.newgamesapi.game.GameManager;
import org.madblock.newgamesapi.game.HubManager;
import org.madblock.newgamesapi.kits.Kit;
import org.madblock.newgamesapi.kits.KitGroup;
import org.madblock.newgamesapi.kits.PlayerKitsManager;

import java.util.Optional;

public class CommandKit extends PluginCommand<NewGamesAPI1> {

    public CommandKit() {
        super("kit", NewGamesAPI1.get());
        this.setDescription("Switches your kit.");
        this.setUsage("/kit <String: kitID>");

        this.commandParameters.clear();
        this.commandParameters.put("default", new CommandParameter[]{
                CommandParameter.newType("kitid", true, CommandParamType.STRING)
        });
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if(!(sender instanceof Player)){
            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You must be a player to use this command.", TextFormat.RED));
            return true;
        }

        if (args.length < 1) {

            GameID gameId = NewGamesAPI1.getGameManager().getPlayerLookup().get(((Player)sender).getUniqueId()).getGameID();

            if (gameId.getGameIdentifier().equals(HubManager.HUB_GAME_ID) || HubManager.get().getHubGames().values().stream().anyMatch(gId -> gId.getGameIdentifier().equals(gameId.getGameIdentifier()))) {
                PlayerKitsManager.get().sendKitGroupSelectionWindow((Player)sender);
                return true;
            }
            PlayerKitsManager.get().sendKitSelectionWindow((Player)sender, gameId.getGameKits());
            return true;
        }

        Player player = (Player) sender;
        GameHandler handler = null;

        for(String hid: GameManager.get().getAllSessionIDs()){
            Optional<GameHandler> h = GameManager.get().getSession(hid);
            if(h.isPresent() && h.get().getPlayers().contains(player)){
                handler = h.get();
                break;
            }
        }
        if(handler == null){
            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You are not in an active game.", TextFormat.RED));
        } else {
            KitGroup selectionPool = handler.getGameID().getGameKits();
            Kit selectedKit = selectionPool.getGroupKits().get(args[0].toLowerCase());
            if(selectedKit == null){
                sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "There is no kit with the id "+args[0]+". Available kits include:", TextFormat.RED));
                for(String id: selectionPool.getGroupKits().keySet()){
                    sender.sendMessage(TextFormat.RED+" - "+id);
                }
            } else {
                if (!PlayerKitsManager.get().playerOwnsKit(player, selectionPool, selectedKit.getKitID())) {
                    sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You have not purchased this kit yet!", TextFormat.RED));
                    return true;
                }

                PlayerKitsManager.get().setPreference(player, selectionPool, selectedKit.getKitID());
                sender.sendMessage(Utility.generateServerMessage("KITS", TextFormat.BLUE, "You equipped the " + TextFormat.BOLD + TextFormat.YELLOW +selectedKit.getKitDisplayName() + TextFormat.RESET + TextFormat.GRAY + " kit!", TextFormat.GRAY));

                // Make sure we aren't in the hub/active game.
                for (GameID gameID : HubManager.get().getHubGames().values()) {
                    if (gameID.getGameServerID() == handler.getGameID().getGameServerID() || handler.getGameState() == GameHandler.GameState.MAIN_LOOP) {
                        return true;
                    }
                }
                selectedKit.applyKit(player, handler, true);
            }
        }

        return true;
    }
}
