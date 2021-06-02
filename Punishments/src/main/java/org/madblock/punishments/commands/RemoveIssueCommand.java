package org.madblock.punishments.commands;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.utils.TextFormat;
import org.madblock.punishments.PunishmentsPlugin;
import org.madblock.punishments.api.PunishmentManager;
import org.madblock.punishments.utils.Utility;
import org.madblock.ranks.api.RankManager;
import org.madblock.ranks.api.RankProfile;

import java.sql.SQLException;
import java.util.Optional;

public class RemoveIssueCommand extends PluginCommand<PunishmentsPlugin> {

    public RemoveIssueCommand(PunishmentsPlugin plugin) {
        super("isremove", plugin);
        this.setDescription("Remove a punishment from a player.");
        this.setUsage("/isremove <id> <reason>");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {

        if (sender.isPlayer()) {
            Optional<RankProfile> rankProfile = RankManager.getInstance().getRankProfile((Player)sender);
            if ((!rankProfile.isPresent() || !(rankProfile.get().hasPermission("punishments.issue.full") || rankProfile.get().hasPermission("punishments.issue.limited")))) {
                sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You do not have permission to execute this command.", TextFormat.RED));
                return true;
            }
        }

        if (args.length < 2) {
            sender.sendMessage(Utility.generateServerMessage("PUNISHMENTS", TextFormat.DARK_RED, this.getUsage(), TextFormat.RED));
            return true;
        }

        int id;
        try {
            id = Integer.parseInt(args[0]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(Utility.generateServerMessage("PUNISHMENTS", TextFormat.DARK_RED, "That was not a valid punishment id", TextFormat.RED));
            return true;
        }

        StringBuilder reasonBuilder = new StringBuilder(args[1]);
        for (int i = 2; i < args.length; i++) {
            reasonBuilder.append(" " + args[i]);
        }
        final String reason = reasonBuilder.toString();

        if (reason.length() > 255) {
            sender.sendMessage(Utility.generateServerMessage("PUNISHMENTS", TextFormat.DARK_RED.DARK_RED, "Your reason is too long.", TextFormat.RED));
            return true;
        }

        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {
            boolean success;
            try {
                success = PunishmentManager.getInstance().removePunishment(id, reason, sender);
            } catch (SQLException exception) {
                exception.printStackTrace();
                getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                        Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Internal Server Error", TextFormat.RED)
                ));
                return;
            }

            getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {
                if (success) {
                    sender.sendMessage(Utility.generateServerMessage("PUNISHMENTS", TextFormat.DARK_RED, "Removed punishment id " + TextFormat.YELLOW + id, TextFormat.GREEN));
                } else {
                    sender.sendMessage(Utility.generateServerMessage("PUNISHMENTS", TextFormat.DARK_RED, "That punishment is not loaded or it was already removed.", TextFormat.RED));
                }
            });
        }, true);
        return true;
    }

}
