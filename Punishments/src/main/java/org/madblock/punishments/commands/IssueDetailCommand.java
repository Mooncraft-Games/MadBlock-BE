package org.madblock.punishments.commands;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.utils.TextFormat;
import org.madblock.punishments.PunishmentsPlugin;
import org.madblock.punishments.api.PunishmentEntry;
import org.madblock.punishments.api.PunishmentManager;
import org.madblock.punishments.utils.Utility;
import org.madblock.ranks.api.RankManager;
import org.madblock.ranks.api.RankProfile;

import java.text.DateFormat;
import java.util.Date;
import java.util.Optional;

public class IssueDetailCommand extends PluginCommand<PunishmentsPlugin> {


    public IssueDetailCommand(PunishmentsPlugin plugin) {
        super("isdetail", plugin);
        this.setUsage("/isdetail <id>");
        this.setDescription("View more information about a punishment.");
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

        int id;
        try {
            id = Integer.parseInt(args[0]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(Utility.generateServerMessage("PUNISHMENTS", TextFormat.DARK_RED, "That was not a valid punishment id", TextFormat.RED));
            return true;
        }

        Optional<PunishmentEntry> entry = PunishmentManager.getInstance().getCachedPunishment(id);

        if (!entry.isPresent()) {
            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "That punishment id was not loaded or does not exist.", TextFormat.RED));
            return true;
        }

        String expiryDate = TextFormat.GREEN + DateFormat.getInstance().format(new Date(entry.get().getExpireAt()));
        if (entry.get().isRemoved()) {
            expiryDate = TextFormat.RED + "Removed";
        } else if (entry.get().isPermanent()) {
            expiryDate = TextFormat.RED + "Permanent";
        }

        if (entry.get().isRemoved()) {
            sender.sendMessage(
                    TextFormat.GREEN + "Loading punishment data for id: #" + id + "..." + TextFormat.WHITE + "\n" +
                            "Issued by: " + TextFormat.AQUA + entry.get().getIssuedBy() + TextFormat.WHITE +  "\n" +
                            "Reason: " + TextFormat.GRAY + entry.get().getReason() + TextFormat.WHITE + "\n" +
                            "Issued on: " + TextFormat.GREEN + DateFormat.getInstance().format(new Date(entry.get().getIssuedAt())) + TextFormat.WHITE + "\n" +
                            "Expires on: " + expiryDate + TextFormat.WHITE + "\n" +
                            "Removed by: " + TextFormat.AQUA + entry.get().getRemovedBy() + TextFormat.WHITE + "\n" +
                            "Removed Reason: " + TextFormat.GRAY + entry.get().getRemovedReason() + TextFormat.WHITE + "\n"
            );
        } else {
            sender.sendMessage(
                    TextFormat.GREEN + "Loading punishment data for id: #" + id + "..." + TextFormat.WHITE + "\n" +
                            "Issued by: " + TextFormat.AQUA + entry.get().getIssuedBy() + TextFormat.WHITE +  "\n" +
                            "Reason: " + TextFormat.GRAY + entry.get().getReason() + TextFormat.WHITE + "\n" +
                            "Issued on: " + TextFormat.GREEN + DateFormat.getInstance().format(new Date(entry.get().getIssuedAt())) + TextFormat.WHITE + "\n" +
                            "Expires on: " + expiryDate
            );
        }

        return true;
    }
}
