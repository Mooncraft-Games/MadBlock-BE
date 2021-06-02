package org.madblock.ranks.commands;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.utils.TextFormat;
import org.madblock.playerregistry.PlayerRegistry;
import org.madblock.ranks.RankPlugin;
import org.madblock.ranks.api.RankManager;
import org.madblock.ranks.api.RankProfile;
import org.madblock.ranks.enums.PrimaryRankID;
import org.madblock.ranks.enums.SubRankID;
import org.madblock.ranks.util.Utility;

import java.sql.SQLException;
import java.util.Optional;

public class RankCommand extends PluginCommand<RankPlugin> {

    public RankCommand(RankPlugin plugin) {
        super("rank", plugin);
        this.setDescription("Add ranks to players");
        this.setUsage(
                "/rank list <sub/primary> <username> - list the ranks a player has\n" +
                        "/rank add <sub/primary> <rank_id> <username> - Add a rank to a player\n" +
                        "/rank remove <sub/primary> <rank_id> <username> - Remove a rank from a player"
        );


        String[] primaryRankIds = new String[PrimaryRankID.values().length];
        int i = 0;
        for (PrimaryRankID rank : PrimaryRankID.values()) {
            primaryRankIds[i] = rank.getRankId();
            i++;
        }
        String[] subRankIds = new String[SubRankID.values().length];
        i = 0;
        for (SubRankID rank : SubRankID.values()) {
            subRankIds[i] = rank.getGroupId();
            i++;
        }
        this.commandParameters.clear();

        // TODO: Make this work properly. Ugh
        this.commandParameters.put("primary", new CommandParameter[]{
                CommandParameter.newEnum("action", new String[]{ "add", "remove" }),
                CommandParameter.newEnum("type", new String[]{ "primary" }),
                CommandParameter.newEnum("id", primaryRankIds),
                CommandParameter.newType("player", CommandParamType.TARGET)
        });
        this.commandParameters.put("sub", new CommandParameter[]{
                CommandParameter.newEnum("action", new String[]{ "add", "remove" }),
                CommandParameter.newEnum("type", new String[]{ "sub" }),
                CommandParameter.newEnum("id", subRankIds),
                CommandParameter.newType("player", CommandParamType.TARGET)
        });
        this.commandParameters.put("list", new CommandParameter[]{
                CommandParameter.newEnum("action", new String[]{ "list" }),
                CommandParameter.newEnum("type", new String[]{ "primary", "sub" }),
                CommandParameter.newType("player", CommandParamType.TARGET)
        });
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {

        if (!sender.isOp()) {

            if (sender.isPlayer()) {
                Optional<RankProfile> profile = RankManager.getInstance().getRankProfile((Player) sender);

                if (!profile.isPresent() || !profile.get().hasPermission("ranks.commands.rank")) {
                    sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You do not have permission to execute this command.", TextFormat.RED));
                    return true;
                }

            } else {
                sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You do not have permission to execute this command.", TextFormat.RED));
                return true;
            }
        }

        // rank list sub dapbillk
        // rank add sub id playerName

        if (
                (args.length < 1) ||
                        // Validate list argument length
                        (args[0].equalsIgnoreCase("list") && args.length < 3) ||
                        // Validate add/remove argument length and add/sub keyword
                        (
                                (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) &&
                                        (args.length < 4 || !(args[1].equalsIgnoreCase("sub") || args[1].equalsIgnoreCase("primary")))
                        )
        ) {
            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Invalid parameters\n" + getUsage(), TextFormat.RED));
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {

            StringBuilder builder = new StringBuilder(args[2]);
            for (int i = 3; i < args.length; i++) {
                builder.append(" ").append(args[i]);
            }
            String playerName = builder.toString();

            getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {
                Optional<String> xuid;
                try {
                    xuid = PlayerRegistry.getPlayerXuidByName(playerName);
                } catch (SQLException exception) {
                    exception.printStackTrace();
                    getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                            Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An internal error has occurred.", TextFormat.RED)
                    ));
                    return;
                }

                if (!xuid.isPresent()) {
                    getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                            Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "That player has not been registered into our systems.", TextFormat.RED)
                    ));
                    return;
                }

                Optional<RankProfile> profile;
                try {
                    profile = RankManager.getInstance().fetchRankProfile(xuid.get());
                } catch (SQLException exception) {
                    exception.printStackTrace();
                    getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                            Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An internal error has occurred.", TextFormat.RED)
                    ));
                    return;
                }

                if (!profile.isPresent()) {
                    getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                            Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "No rank profile found.", TextFormat.RED)
                    ));
                    return;
                }

                getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {

                    StringBuilder ranks = new StringBuilder();
                    switch (args[1].toLowerCase()) {
                        case "primary":
                            for (PrimaryRankID rank : profile.get().getPrimaryRanks()) {
                                ranks.append(rank.getRankId()).append(" ");
                            }
                            break;
                        case "sub":
                            for (SubRankID rank : profile.get().getSubRanks()) {
                                ranks.append(rank.getGroupId()).append(" ");
                            }
                            break;
                        default:
                            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You must type either primary or sub", TextFormat.RED));
                            return;
                    }
                    sender.sendMessage(
                            Utility.generateServerMessage("RANKS", TextFormat.YELLOW, String.format("%s%s%s has the ranks: %s", TextFormat.AQUA, playerName, TextFormat.GREEN, ranks.toString()))
                    );
                });

            }, true);

        } else {
            // add/remove

            if (!(args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
                sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Invalid parameters\n" + getUsage(), TextFormat.RED));
                return true;
            }

            if (!(args[1].equalsIgnoreCase("sub") || args[1].equalsIgnoreCase("primary"))) {
                sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Invalid parameters\n" + getUsage(), TextFormat.RED));
                return true;
            }

            StringBuilder builder = new StringBuilder(args[3]);
            for (int i = 4; i < args.length; i++) {
                builder.append(" ").append(args[i]);
            }
            String playerName = builder.toString();

            getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {
                Optional<String> xuid;
                try {
                    xuid = PlayerRegistry.getPlayerXuidByName(playerName);
                } catch (SQLException exception) {
                    exception.printStackTrace();
                    getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                            Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An internal error has occurred.", TextFormat.RED)
                    ));
                    return;
                }

                if (!xuid.isPresent()) {
                    getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                            Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "That player has not been registered into our systems.", TextFormat.RED)
                    ));
                    return;
                }

                Optional<RankProfile> profile;
                try {
                    profile = RankManager.getInstance().fetchRankProfile(xuid.get());
                } catch (SQLException exception) {
                    exception.printStackTrace();
                    getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                            Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An internal error has occurred.", TextFormat.RED)
                    ));
                    return;
                }

                if (!profile.isPresent()) {
                    getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                            Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Internal Server Error", TextFormat.RED)
                    ));
                    return;
                }

                if (args[1].equalsIgnoreCase("sub")) {

                    SubRankID subRankID = null;
                    for (SubRankID rank : SubRankID.values()) {
                        if (rank.getGroupId().equalsIgnoreCase(args[2])) {
                            subRankID = rank;
                        }
                    }

                    if (subRankID == null) {
                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                                Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "That is not a valid sub rank.", TextFormat.RED)
                        ));
                        return;
                    }
                    if (args[0].equalsIgnoreCase("add")) {
                        if (profile.get().hasSubRank(subRankID)) {
                            getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                                    Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "The player already has this sub rank.", TextFormat.RED)
                            ));
                            return;
                        }
                        boolean success;
                        try {
                            success = profile.get().addSubRank(subRankID);
                        } catch (SQLException exception) {
                            exception.printStackTrace();
                            getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                                    Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Internal Server Error", TextFormat.RED)
                            ));
                            return;
                        }

                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {
                            if (success) {
                                sender.sendMessage(Utility.generateServerMessage("RANKS", TextFormat.YELLOW, "Successfully added sub rank.", TextFormat.GREEN));
                            } else {
                                sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Internal Server Error", TextFormat.RED));
                            }
                        });

                    } else {
                        if (!profile.get().hasSubRank(subRankID)) {
                            getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                                    Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "The player does not have this sub rank.", TextFormat.RED)
                            ));
                            return;
                        }

                        boolean success;
                        try {
                            success = profile.get().removeSubRank(subRankID);
                        } catch (SQLException exception) {
                            exception.printStackTrace();
                            getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                                    Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Internal Server Error", TextFormat.RED)
                            ));
                            return;
                        }

                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {
                            if (success) {
                                sender.sendMessage(Utility.generateServerMessage("RANKS", TextFormat.YELLOW, "Successfully removed sub rank.", TextFormat.GREEN));
                            } else {
                                sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Internal Server Error", TextFormat.RED));
                            }
                        });

                    }
                } else {

                    PrimaryRankID primaryRankID = null;
                    for (PrimaryRankID rank : PrimaryRankID.values()) {
                        if (rank.getRankId().equalsIgnoreCase(args[2])) {
                            primaryRankID = rank;
                        }
                    }
                    if (primaryRankID == null) {
                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                                Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "That is not a valid primary rank.", TextFormat.RED)
                        ));
                        return;
                    }

                    if (args[0].equalsIgnoreCase("add")) {
                        if (profile.get().hasPrimaryRank(primaryRankID)) {
                            getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                                    Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "The player already has this primary rank.", TextFormat.RED)
                            ));
                            return;
                        }
                        boolean success;
                        try {
                            success = profile.get().addPrimaryRank(primaryRankID);
                        } catch (SQLException exception) {
                            exception.printStackTrace();
                            getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                                    Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Internal Server Error", TextFormat.RED)
                            ));
                            return;
                        }

                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {
                            if (success) {
                                sender.sendMessage(Utility.generateServerMessage("RANKS", TextFormat.YELLOW, "Successfully added primary rank.", TextFormat.GREEN));
                            } else {
                                sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Internal Server Error", TextFormat.RED));
                            }
                        });

                    } else {
                        if (!profile.get().hasPrimaryRank(primaryRankID)) {
                            getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                                    Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "The player does not have this primary rank.", TextFormat.RED)
                            ));
                            return;
                        }

                        if (primaryRankID.equals(PrimaryRankID.PLAYER)) {
                            getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                                    Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You cannot remove the player primary rank.", TextFormat.RED)
                            ));
                            return;
                        }

                        boolean success;
                        try {
                            success = profile.get().removePrimaryRank(primaryRankID);
                        } catch (SQLException exception) {
                            exception.printStackTrace();
                            getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                                    Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Internal Server Error", TextFormat.RED)
                            ));
                            return;
                        }

                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {
                            if (success) {
                                sender.sendMessage(Utility.generateServerMessage("RANKS", TextFormat.YELLOW, "Successfully removed primary rank.", TextFormat.GREEN));
                            } else {
                                sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Internal Server Error", TextFormat.RED));
                            }
                        });

                    }

                }

            }, true);


        }
        return true;
    }
}