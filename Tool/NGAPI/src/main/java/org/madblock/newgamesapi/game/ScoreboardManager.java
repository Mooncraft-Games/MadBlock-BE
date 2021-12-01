package org.madblock.newgamesapi.game;

import cn.nukkit.Player;
import cn.nukkit.utils.TextFormat;
import de.lucgameshd.scoreboard.api.ScoreboardAPI;
import de.lucgameshd.scoreboard.network.DisplayEntry;
import de.lucgameshd.scoreboard.network.DisplaySlot;
import de.lucgameshd.scoreboard.network.Scoreboard;
import de.lucgameshd.scoreboard.network.ScoreboardDisplay;
import org.madblock.newgamesapi.Utility;

import java.util.*;

public class ScoreboardManager {

    protected GameHandler gameHandler;

    protected Map<Player, ScoreboardDisplay> scoreboards;                     // Stores scoreboards
    protected Map<Player, Map<Integer, DisplayEntry>> scoreboardEntries; // Stores entries for a player's scoreboard
    protected Map<Player, Map<Integer, String>> scoreboardChanges;            // Stores changes that need to be made for a client

    public ScoreboardManager(GameHandler handler) {
        this.gameHandler = handler;
        this.scoreboards = new HashMap<>();
        this.scoreboardEntries = new HashMap<>();
        this.scoreboardChanges = new HashMap<>();
    }

    /**
     * Set the line of the player's scoreboard.
     *
     * @param line The LOWER this is, the more priority it has over other lines
     * @param info If null, it will delete the line
     */
    public void setLine(Player player, int line, String info) {

        this.scoreboardChanges.putIfAbsent(player, new HashMap<>());
        if (info == null) {

            // Schedule this line for deletion
            this.scoreboardChanges.get(player).put(line, null);

        } else {

            // Schedule this line to be added
            this.scoreboardChanges.get(player).put(line, info);

        }

        this.updateScoreboards();
    }

    /**
     * Retrieve a line from a player's scoreboard
     *
     * @param line
     * @return the line if it exists
     */
    public Optional<String> getLine(Player player, int line) {
        if (this.scoreboards.containsKey(player)) {

            // Use the most recent line change
            if (this.scoreboardChanges.containsKey(player) && this.scoreboardChanges.get(player).containsKey(line)) {
                return Optional.ofNullable(this.scoreboardChanges.get(player).get(line));
            } else {
                return Optional.of(this.scoreboards.get(player).getLine(line));
            }

        } else {
            return Optional.empty();
        }
    }

    public void cleanUp(Player player) {
        this.scoreboardChanges.remove(player);
        if (this.scoreboards.containsKey(player)) {

            for (DisplayEntry entry : this.scoreboardEntries.get(player).values()) {
                this.scoreboards.get(player).removeEntry(entry);
            }
            this.scoreboardEntries.remove(player);
            ScoreboardAPI.removeScorebaord(player, this.scoreboards.get(player).getScoreboard());
            this.scoreboards.remove(player);

        }
    }

    protected void updateScoreboards() {
        for (Player player : scoreboardChanges.keySet()) {

            Map<Integer, String> changes = scoreboardChanges.get(player);

            for (int line : changes.keySet()) {

                if (changes.get(line) == null && this.scoreboardEntries.get(player).containsKey(line)) {

                    // We are removing a line
                    this.scoreboards.get(player).removeEntry(this.scoreboardEntries.get(player).get(line));
                    this.scoreboardEntries.get(player).remove(line);

                } else if (changes.get(line) != null) {

                    // We are adding a line

                    // Create the scoreboard if it doesn't exist
                    if (!this.scoreboards.containsKey(player)) {
                        Scoreboard scoreboard = ScoreboardAPI.createScoreboard();
                        ScoreboardDisplay display = scoreboard.addDisplay(DisplaySlot.SIDEBAR,  String.format("scoreboard_%s_%s", gameHandler.getGameID().getGameIdentifier(), Utility.generateUniqueToken(6, 4)), String.format("%s%sMooncraft %s%sGames", TextFormat.BLUE, TextFormat.BOLD, TextFormat.DARK_AQUA, TextFormat.BOLD));
                        scoreboards.put(player, display);
                        scoreboardEntries.put(player, new HashMap<>());
                        ScoreboardAPI.setScoreboard(player, scoreboard);
                    }

                    if (scoreboardEntries.get(player).containsKey(line)) {
                        scoreboards.get(player).removeEntry(scoreboardEntries.get(player).get(line));
                    }

                    scoreboardEntries.get(player).put(line, scoreboards.get(player).addLine(changes.get(line), line));

                }

            }

            // Do we need to remove the scoreboard now?
            if (this.scoreboards.get(player).getLineEntry().size() == 0) {
                ScoreboardAPI.removeScorebaord(player, this.scoreboards.get(player).getScoreboard());
                this.scoreboards.remove(player);
            }

        }
        scoreboardChanges.clear();
    }

}
