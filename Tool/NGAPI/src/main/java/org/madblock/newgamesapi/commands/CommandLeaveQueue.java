package org.madblock.newgamesapi.commands;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.QuiccccQueueManager;

/**
 * @author Nicholas
 */
public class CommandLeaveQueue extends PluginCommand<NewGamesAPI1> {
    public CommandLeaveQueue() {
        super("leavequeue", NewGamesAPI1.get());
        setDescription("Removes you from a game's queue.");
        setUsage("/leavequeue");
        setAliases(new String[] { "lq" });

        commandParameters.clear();
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.isPlayer()) {
            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You must be a player to execute this command.", TextFormat.RED));
            return true;
        }

        Player player = (Player) sender;
        QuiccccQueueManager queueManager = QuiccccQueueManager.get();
        if (queueManager != null && queueManager.isInQueue(player)) {
            queueManager.leaveQueue(player);
            player.sendMessage(Utility.generateServerMessage("QUEUE", TextFormat.GOLD, "You left your current queue.", TextFormat.GRAY));
        } else {
            player.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "There was an error processing your request. " +
                    "Are you queued?", TextFormat.RED));
        }

        return true;
    }
}