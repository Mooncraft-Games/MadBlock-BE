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
import org.madblock.newgamesapi.rewards.PlayerRewardsProfile;
import org.madblock.newgamesapi.rewards.RewardChunk;
import org.madblock.newgamesapi.rewards.RewardsManager;
import org.madblock.playerregistry.PlayerRegistry;
import org.madblock.ranks.api.RankManager;
import org.madblock.ranks.api.RankProfile;
import org.madblock.util.DatabaseReturn;

import java.sql.SQLException;
import java.util.Optional;

public class CommandAddStat extends PluginCommand<NewGamesAPI1> {

    public CommandAddStat() {
        super("addstat", NewGamesAPI1.get());

        this.setDescription("Give stats to players");
        this.setUsage("/addstat <amount> <type> <player name>");

        this.commandParameters.clear();
        this.commandParameters.put("amount", new CommandParameter[]{
                CommandParameter.newType("amount", CommandParamType.INT),
                CommandParameter.newEnum("type", new CommandEnum("Type","coins", "experience", "trophies")),
                CommandParameter.newType("player_name", CommandParamType.TARGET)
        });

    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {

        if (sender.isPlayer()) {
            Optional<RankProfile> profile = RankManager.getInstance().getRankProfile((Player)sender);
            if (!profile.isPresent() || !profile.get().hasPermission("newgameapi.commands.addstat")) {
                sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You do not have permission to execute this command.", TextFormat.RED));
                return true;
            }
        }

        if (args.length < 3) {
            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Invalid usage: " + this.getUsage(), TextFormat.RED));
            return true;
        }

        int amount;
        try {
            amount = Integer.valueOf(args[0]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Invalid number.", TextFormat.RED));
            return true;
        }

        StringBuilder builder = new StringBuilder(args[2]);
        for (int i = 3; i < args.length; i++) {
            builder.append(" ").append(args[i]);
        }

        String playerName = builder.toString();

        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> {
            DatabaseReturn<String> xuid;
            try {
                xuid = PlayerRegistry.getPlayerXuidByName(playerName);
            } catch (SQLException exception) {
                getPlugin().getLogger().error(String.format("Failed to get xuid for %s when adding stats via /addstat", playerName));
                getPlugin().getLogger().error(exception.toString());
                getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                        Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An internal error has occurred.", TextFormat.RED)
                ));
                return;
            }
            if (!xuid.isPresent()) {
                getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                        Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "That user does not exist.", TextFormat.RED)
                ));
                return;
            }
            try {
                PlayerRewardsProfile rewards = RewardsManager.get().fetchRewards(xuid.get());
                switch (args[1].toLowerCase()) {
                    case "coins": {
                        boolean success = rewards.addRewards(
                                new RewardChunk("cheater", "Cheater", 0, amount)
                        );
                        if (!success) {
                            getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                                    Utility.generateServerMessage("STATS", TextFormat.YELLOW,
                                        "Unable to add coins",
                                        TextFormat.RED
                                    )
                            ));
                            return;
                        }
                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                                Utility.generateServerMessage("STATS", TextFormat.YELLOW,
                                        String.format("Added %s coins to the account of %s", amount, playerName),
                                        TextFormat.GREEN
                                )
                        ));
                    }
                    break;
                    case "experience":
                    case "xp": {
                        boolean success = rewards.addRewards(
                                new RewardChunk("cheater", "Cheater", amount, 0)
                        );
                        if (!success) {
                            getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                                    Utility.generateServerMessage("STATS", TextFormat.YELLOW,
                                            "Unable to add experience",
                                            TextFormat.RED
                                    )
                            ));
                            return;
                        }
                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                                Utility.generateServerMessage("STATS", TextFormat.YELLOW,
                                    String.format("Added %s experience to the account of %s", amount, playerName),
                                    TextFormat.GREEN
                                )
                        ));
                    }
                    break;
                    case "trophies":
                    case "trophy":
                        boolean success = rewards.addRewards(
                                new RewardChunk("cheater", "Cheater", 0, 0, amount)
                        );
                        if (!success) {
                            getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                                    Utility.generateServerMessage("STATS", TextFormat.YELLOW,
                                            "Unable to add trophies",
                                            TextFormat.RED
                                    )
                            ));
                            return;
                        }
                        getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                                Utility.generateServerMessage("STATS", TextFormat.YELLOW,
                                        String.format("Added %s trophies to the account of %s", amount, playerName),
                                        TextFormat.GREEN
                                )
                        ));
                    break;
                    default:
                        sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "That is not a valid type of stat.", TextFormat.RED));
                        break;
                }
            } catch (SQLException exception) {
                getPlugin().getLogger().error(String.format("Failed to add stats via /addstat", playerName));
                getPlugin().getLogger().error(exception.toString());
                getPlugin().getServer().getScheduler().scheduleTask(getPlugin(), () -> sender.sendMessage(
                        Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An internal error has occurred.", TextFormat.RED)
                ));
                return;
            }

        }, true);

        return true;

    }
}
