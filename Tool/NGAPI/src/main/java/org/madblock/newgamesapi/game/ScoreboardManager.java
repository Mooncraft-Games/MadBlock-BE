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

    protected final GameHandler gameHandler;

    protected final Map<Player, ScoreboardDisplay> scoreboards = new HashMap<>();                     // Stores scoreboards
    protected final Map<Player, Map<Integer, DisplayEntry>> scoreboardEntries = new HashMap<>();      // Stores entries for a player's scoreboard

    public ScoreboardManager(GameHandler handler) {
        this.gameHandler = handler;
    }

    /**
     * Set the line of the player's scoreboard.
     *
     * @param line The LOWER this is, the more priority it has over other lines
     * @param info If null, it will delete the line
     */
    public void setLine(Player player, int line, String info) {
        if (this.scoreboardEntries.containsKey(player) && this.scoreboardEntries.get(player).containsKey(line) && info == null) {
            // We are removing a line
            this.scoreboardEntries.get(player).remove(line);
            this.scoreboards.get(player).removeEntry(this.scoreboardEntries.get(player).get(line));

            // Check if we can garbage cleanup
            if (this.scoreboards.get(player).getLineEntry().size() == 0) {
                ScoreboardAPI.removeScorebaord(player, this.scoreboards.get(player).getScoreboard());
                this.scoreboards.remove(player);
            }
        } else if (info != null) {
            // We are adding a line
            // Create the scoreboard if it doesn't exist
            if (!this.scoreboards.containsKey(player)) {
                Scoreboard scoreboard = ScoreboardAPI.createScoreboard();
                ScoreboardDisplay display = scoreboard.addDisplay(DisplaySlot.SIDEBAR,  String.format("scoreboard_%s_%s", gameHandler.getGameID().getGameIdentifier(), Utility.generateUniqueToken(6, 4)), String.format("%s%sMooncraft %s%sGames", TextFormat.BLUE, TextFormat.BOLD, TextFormat.DARK_AQUA, TextFormat.BOLD));
                ScoreboardAPI.setScoreboard(player, scoreboard);

                this.scoreboards.put(player, display);
                this.scoreboardEntries.put(player, new HashMap<>());
            }

            // Delete the existing line if any exists and then add the new line
            if (this.scoreboardEntries.get(player).containsKey(line)) {
                this.scoreboards.get(player).removeEntry(this.scoreboardEntries.get(player).get(line));
            }
            this.scoreboardEntries.get(player).put(line, scoreboards.get(player).addLine(info, line));

        }
    }

    /**
     * Retrieve a line from a player's scoreboard
     *
     * @param line
     * @return the line if it exists
     */
    public Optional<String> getLine(Player player, int line) {
        if (this.scoreboards.containsKey(player)) {
            return Optional.of(this.scoreboards.get(player).getLine(line));
        } else {
            return Optional.empty();
        }
    }

    public void cleanUp(Player player) {
        if (this.scoreboards.containsKey(player)) {
            for (DisplayEntry entry : this.scoreboardEntries.get(player).values()) {
                this.scoreboards.get(player).removeEntry(entry);
            }
            this.scoreboardEntries.remove(player);
            ScoreboardAPI.removeScorebaord(player, this.scoreboards.get(player).getScoreboard());
            this.scoreboards.remove(player);

        }
    }

}
