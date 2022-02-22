package org.madblock.newgamesapi.commands;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.command.data.CommandEnum;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.utils.TextFormat;
import org.madblock.database.ConnectionWrapper;
import org.madblock.database.DatabaseAPI;
import org.madblock.database.DatabaseStatement;
import org.madblock.database.DatabaseUtility;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.util.Utility;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.game.GameManager;
import org.madblock.newgamesapi.rewards.PlayerRewardsProfile;
import org.madblock.newgamesapi.rewards.RewardsManager;
import org.madblock.ranks.api.RankManager;
import org.madblock.ranks.api.RankProfile;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class CommandTourney extends PluginCommand<NewGamesAPI1> {

    public static final String CLEAR_TOURNEY_USER_SQL = "UPDATE `player_rewards` SET tourney=0 WHERE player_rewards.xuid=?;";
    public static final String CLEAR_TOURNEY_SQL = "UPDATE player_rewards SET tourney=0;";
    public static final String TOURNEY_LEADERBOARD = "SELECT player_lookup.username, player_rewards.tourney FROM `player_rewards` INNER JOIN `player_lookup` ON player_rewards.xuid = player_lookup.xuid WHERE player_rewards.tourney > 0 ORDER BY player_rewards.tourney DESC;";


    public CommandTourney() {
        super("tourney", NewGamesAPI1.get());
        this.setDescription("Handles tourney games");
        this.setUsage("/tourney master <String: gameid> \nOR /tourney start <String: sessionid> \nOR /tourney list \nOR /tourney reset");

        this.commandParameters.clear();
        this.commandParameters.put("master", new CommandParameter[]{
                CommandParameter.newEnum("master", new CommandEnum("Master", "master")),
                CommandParameter.newType("gameid", CommandParamType.STRING)
        });
        this.commandParameters.put("start", new CommandParameter[]{
                CommandParameter.newEnum("start", new CommandEnum("Start", "start")),
                CommandParameter.newType("sessionid", true, CommandParamType.STRING)
        });
        this.commandParameters.put("list", new CommandParameter[]{
                CommandParameter.newEnum("list", new CommandEnum("List", "list"))
        });
        this.commandParameters.put("reset", new CommandParameter[]{
                CommandParameter.newEnum("reset", new CommandEnum("Reset", "reset"))
        });
        this.commandParameters.put("board", new CommandParameter[]{
                CommandParameter.newEnum("board", new CommandEnum("Board", "board"))
        });
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {

        if (sender.isPlayer()) {

            if (args.length < 1) {
                sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Invalid Usage", TextFormat.RED));
                return true;
            }

            if (!args[0].equalsIgnoreCase("board")) { // Tourney board is the only public command

                Optional<RankProfile> profile = RankManager.getInstance().getRankProfile((Player) sender);

                if (!profile.isPresent() || !profile.get().hasPermission("newgameapi.commands.tourney")) {
                    sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You do not have permission to execute this command.", TextFormat.RED));
                    return true;
                }
            }
        }

        if (args.length < 1) {
            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, String.format("Invalid parameters (At least 1 param expected. Found %s): %s", args.length, getUsage()), TextFormat.RED));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "master":

                if (sender instanceof Player) {

                    if ((args.length > 1)) {
                        String id = args[1].toLowerCase();

                        Optional<GameHandler> session = GameManager.get().getSession(id);

                        if (session.isPresent() && session.get().getGameID().getGameProperties().isTourneyGamemode()) {
                            sender.sendMessage(Utility.generateServerMessage("TOURNEY", TextFormat.GOLD, "Mastering " + args[1]));
                            session.get().registerTourneymasterToGame(((Player) sender));

                        } else {
                            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, String.format("Tourney Session with id [%s] does not exist", args[1]), TextFormat.RED));
                        }

                    } else {
                        sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Missing arg 'session id'.", TextFormat.RED));
                    }

                } else {
                    sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You must be a player to run that command.", TextFormat.RED));
                }
                return true;


            case "start":

                if (args.length > 1) {

                    String id = args[1].toLowerCase();

                    Optional<GameHandler> home = GameManager.get().getSession(id);

                    if (home.isPresent() && home.get().getGameID().getGameProperties().isTourneyGamemode()) {
                        sender.sendMessage(Utility.generateServerMessage("TOURNEY", TextFormat.GOLD, "Starting tourney: " + id));
                        home.get().setTourneyStarted(true);

                    } else {
                        sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, String.format("Tourney Session with id [%s] does not exist", args[1]), TextFormat.RED));
                    }

                } else {

                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        GameHandler home = GameManager.get().getPlayerLookup().get(p.getUniqueId());

                        if (home != null && home.getGameID().getGameProperties().isTourneyGamemode()) {
                            sender.sendMessage(Utility.generateServerMessage("TOURNEY", TextFormat.GOLD, "Starting tourney: " + home.getServerID()));
                            home.setTourneyStarted(true);

                        } else {
                            sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You are not currently in a tourney. Join a tourney or specify a session id.", TextFormat.RED));
                        }

                    } else {
                        sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You must be a player to run that command (without specifying a game id)", TextFormat.RED));
                    }
                }
                return true;


            case "list":
                StringBuilder builder = new StringBuilder();
                sender.sendMessage(Utility.generateServerMessage("TOURNEY", TextFormat.GOLD, "Listing all tourneys:", TextFormat.YELLOW));
                for (String id : GameManager.get().getAllSessionIDs()) {
                    GameManager.get().getSession(id).ifPresent(s -> {
                        if (s.getGameID().getGameProperties().isTourneyGamemode() && !((s.getGameState() == GameHandler.GameState.PRE_PREPARE) || (s.getGameState() == GameHandler.GameState.END)))
                            builder.append(TextFormat.GRAY + " - ").append(id);
                    });
                }
                sender.sendMessage(builder.toString());
                return true;


            case "reset":
                List<String> xuids = RewardsManager.get().getCachedXUIDs();

                for (String id : xuids) {
                    PlayerRewardsProfile p = RewardsManager.get().getRewards(id).get();
                    p.quietlyResetTourney(); // Any new saves should now be 0
                }


                ConnectionWrapper wrapper = null;
                PreparedStatement stmt = null;


                try {
                    wrapper = DatabaseAPI.getConnection("MAIN");
                    stmt = wrapper.prepareStatement(new DatabaseStatement(CLEAR_TOURNEY_SQL));

                    stmt.execute();

                    boolean result = stmt.executeUpdate() > 0;
                    stmt.close();

                    sender.sendMessage(Utility.generateServerMessage("TOURNEY", TextFormat.GOLD, "Reset all tourney points!", TextFormat.YELLOW));
                } catch (SQLException e) {
                    sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An error occurred when accessing the database.", TextFormat.RED));
                    e.printStackTrace();
                } finally {
                    if (stmt != null) DatabaseUtility.closeQuietly(stmt);
                    if (wrapper != null) DatabaseUtility.closeQuietly(wrapper);
                }
                return true;

            case "resetself":

                if (sender instanceof Player) {
                    Player pSend = (Player) sender;
                    String xuid = pSend.getLoginChainData().getXUID();
                    // Any new saves should now be 0
                    if((xuid != null) && (xuid.length() > 0))
                        RewardsManager.get()
                                .getRewards(pSend.getLoginChainData().getXUID())
                                .ifPresent(PlayerRewardsProfile::quietlyResetTourney);

                    ConnectionWrapper wrapper2 = null;
                    PreparedStatement stmt2 = null;


                    try {
                        wrapper2 = DatabaseAPI.getConnection("MAIN");
                        stmt2 = wrapper2.prepareStatement(new DatabaseStatement(CLEAR_TOURNEY_USER_SQL));

                        stmt2.execute();

                        boolean result = stmt2.executeUpdate() > 0;
                        stmt2.close();

                        sender.sendMessage(Utility.generateServerMessage("TOURNEY", TextFormat.GOLD, "Reset tourney points for self!", TextFormat.YELLOW));
                    } catch (SQLException e) {
                        sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An error occurred when accessing the database.", TextFormat.RED));
                        e.printStackTrace();
                    } finally {
                        if (stmt2 != null) DatabaseUtility.closeQuietly(stmt2);
                        if (wrapper2 != null) DatabaseUtility.closeQuietly(wrapper2);
                    }

                } else {
                    sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "You must be a player to run that command.", TextFormat.RED));
                }

                return true;


            case "board":
                sendTourneyLeaderboard(sender);
                return true;


            default:
                sender.sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Invalid parameters (1st Param Invalid): " + getUsage(), TextFormat.RED));
                return true;
        }
    }


    public static void sendTourneyLeaderboard(CommandSender sender) {
        NewGamesAPI1.get().getServer().getScheduler().scheduleTask(NewGamesAPI1.get(), () -> {

            try {
                ConnectionWrapper w = DatabaseAPI.getConnection("MAIN");
                PreparedStatement s = null;

                try {
                    s = w.prepareStatement(new DatabaseStatement(TOURNEY_LEADERBOARD));
                    ResultSet results = s.executeQuery();

                    ArrayList<String> sortedPoints = new ArrayList<>();
                    HashMap<String, ArrayList<String>> pointGroups = new HashMap<>();
                    // points: usernames

                    while (results.next()) {
                        String name = results.getString(1); // 1 is first apparently :)))
                        String points = String.valueOf(results.getInt(2));

                        if (!pointGroups.containsKey(points)) {
                            pointGroups.put(points, new ArrayList<>()); // Create a point group if not present
                            sortedPoints.add(points);
                        }

                        pointGroups.get(points).add(name); // Add player to point group
                    }

                    NewGamesAPI1.get().getServer().getScheduler().scheduleTask(NewGamesAPI1.get(), () -> {

                        ArrayList<String> paragraphs = new ArrayList<>();

                        paragraphs.add(" ");
                        paragraphs.add(
                                String.format("%s %s%sMad%s%sBlock %s%sTourney Leaderboard %s",
                                        Utility.ResourcePackCharacters.TROPHY,
                                        TextFormat.DARK_RED,
                                        TextFormat.BOLD,
                                        TextFormat.RED,
                                        TextFormat.BOLD,
                                        TextFormat.GOLD,
                                        TextFormat.BOLD,
                                        Utility.ResourcePackCharacters.TROPHY
                                ));
                        paragraphs.add(" ");

                        int place = 1;

                        for (int i = 0; (i < sortedPoints.size()); i++) {
                            ArrayList<String> pointGroup = pointGroups.get(sortedPoints.get(i));

                            if (i < 3) {
                                paragraphs.add(generateLeaderboardEntry(place, pointGroup.toArray(new String[0]), sortedPoints.get(i)));
                            } else { // Only for tracking individual player's places
                                if (!(sender instanceof Player)) return;
                                Player player = (Player) sender;

                                if (pointGroup.contains(player.getName())) {
                                    paragraphs.add(" ");
                                    paragraphs.add(generateLeaderboardEntry(place, pointGroup.toArray(new String[0]), sortedPoints.get(i)));
                                }
                            }

                            place++;
                        }

                        paragraphs.add(" ");
                        sender.sendMessage(Utility.generateUnlimitedParagraph(paragraphs.toArray(new String[0]), TextFormat.GOLD, TextFormat.GRAY, 35));
                    });

                } finally {
                    if (s != null) {
                        DatabaseUtility.closeQuietly(s);
                    }
                    DatabaseUtility.closeQuietly(w);
                }

            } catch (SQLException err) {
                err.printStackTrace();
                NewGamesAPI1.get().getServer().getScheduler().scheduleTask(NewGamesAPI1.get(), () -> sender.sendMessage(
                        Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An error occurred when accessing the database.", TextFormat.RED)
                ));
                return;
            }
        }, true);
    }

    private static String generateLeaderboardEntry(int place, String[] names, String points) {
        StringBuilder b = new StringBuilder();

        switch (place) {
            case 1:
                b.append(TextFormat.GOLD).append(TextFormat.BOLD).append("#1 ");
                break;
            case 2:
                b.append(TextFormat.GRAY).append(TextFormat.BOLD).append("#2 ");
                break;
            case 3:
                b.append(TextFormat.RED).append(TextFormat.BOLD).append("#3 ");
                break;
            default:
                b.append(TextFormat.DARK_GRAY).append(TextFormat.BOLD).append(String.format("You: #%s ", place));
                break;
        }

        b.append(TextFormat.RESET).append(points).append(Utility.ResourcePackCharacters.TROPHY);

        for (int i = 0; i < names.length; i++) {
            b.append(names[i]);

            if (i < (names.length - 1)) {
                b.append(", ");
            }
        }

        return b.toString();
    }

}
