package org.madblock.punishments.commands;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.utils.TextFormat;
import org.madblock.playerregistry.PlayerRegistry;
import org.madblock.punishments.PunishmentsPlugin;
import org.madblock.punishments.api.PunishmentEntry;
import org.madblock.punishments.api.PunishmentManager;
import org.madblock.punishments.utils.Utility;
import org.madblock.ranks.api.RankManager;
import org.madblock.ranks.api.RankProfile;
import org.madblock.util.DatabaseReturn;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class IssuesCommand extends PluginCommand<PunishmentsPlugin> {


    public IssuesCommand(PunishmentsPlugin plugin) {
        super("issues", plugin);
        this.setDescription("View any issued punishments about a player.");
        this.setUsage("/issues <player name>");

    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {

        if (args.length < 1) {
            sender.sendMessage(Utility.generateServerMessage("PUNISHMENTS", TextFormat.DARK_RED, this.getUsage(), TextFormat.RED));
            return true;
        }

        if (sender.isPlayer()) {
            Optional<RankProfile> rankProfile = RankManager.getInstance().getRankProfile((Player)sender);

            if ((!rankProfile.isPresent() || !(rankProfile.get().hasPermission("punishments.issue.full") || rankProfile.get().hasPermission("punishments.issue.limited")))) {
                sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You do not have permission to execute this command.", TextFormat.RED));
                return true;
            }
        }

        StringBuilder targetNameBuilder = new StringBuilder(args[0]);
        for (int i = 1; i < args.length; i++) {
            targetNameBuilder.append(" " + args[i]);
        }
        final String targetName = targetNameBuilder.toString();

        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {
            DatabaseReturn<String> xuid;
            try {
                xuid = PlayerRegistry.getPlayerXuidByName(targetName);
            } catch (SQLException exception) {
                exception.printStackTrace();
                getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                        Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Internal Server Error", TextFormat.RED)
                ));
                return;
            }

            if (!xuid.isPresent()) {
                getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                        Utility.generateServerMessage("PUNISHMENTS", TextFormat.DARK_RED, "The player is not registered in our database.", TextFormat.RED)
                ));
                return;
            }

            List<PunishmentEntry> punishments;
            try {
                punishments = PunishmentManager.getInstance().getPunishments(xuid.get());
            } catch (SQLException exception) {
                exception.printStackTrace();
                getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                        Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Internal Server Error", TextFormat.RED)
                ));
                return;
            }

            StringBuilder punishmentMessage = new StringBuilder()
                    .append("Showing punishment history of " + TextFormat.GREEN + targetName + TextFormat.RESET);
            for (PunishmentEntry punishment : punishments) {
                String status;
                if (punishment.isRemoved()) {
                    status = TextFormat.RED + "[Removed]";
                } else if (punishment.isExpired()) {
                    status = TextFormat.RED + "[Expired]";
                } else {
                    status = TextFormat.GREEN + "[ACTIVE]";
                }
                punishmentMessage.append("\n" + TextFormat.YELLOW + "[#" + punishment.getId() + "] " + TextFormat.RED + "[" + punishment.getCode() + "] " + TextFormat.GRAY + punishment.getReason() + TextFormat.RESET + TextFormat.AQUA + " - " + punishment.getIssuedBy() + " " + status);
            }
            getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(punishmentMessage.toString()));

        }, true);

        return true;
    }
}
