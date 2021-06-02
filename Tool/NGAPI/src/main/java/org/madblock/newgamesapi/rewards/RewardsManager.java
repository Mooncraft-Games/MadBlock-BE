package org.madblock.newgamesapi.rewards;

import cn.nukkit.Player;
import org.madblock.database.ConnectionWrapper;
import org.madblock.database.DatabaseAPI;
import org.madblock.database.DatabaseStatement;
import org.madblock.database.DatabaseUtility;
import org.madblock.newgamesapi.NewGamesAPI1;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class RewardsManager {

    private static final String CREATE_REWARDS_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS player_rewards ( xuid VARCHAR(16) PRIMARY KEY, experience INT, coins INT, tourney INT );";
    private static final String GET_REWARDS_QUERY = "SELECT experience, coins, tourney FROM player_rewards WHERE xuid=?";

    private static RewardsManager instance;

    private final Map<String, PlayerRewardsProfile> cache = new HashMap<>();

    public RewardsManager () {
        ConnectionWrapper wrapper = null;
        PreparedStatement stmt = null;
        try {
            wrapper = DatabaseAPI.getConnection("MAIN");
            stmt = wrapper.prepareStatement(new DatabaseStatement(CREATE_REWARDS_TABLE_QUERY));
            stmt.execute();
        } catch (SQLException exception) {
            exception.printStackTrace();
            NewGamesAPI1.get().getLogger().critical("Unable to create rewards table.");
        } finally {
            if (stmt != null) {
                DatabaseUtility.closeQuietly(stmt);
            }
            if (wrapper != null) {
                DatabaseUtility.closeQuietly(wrapper);
            }
        }
    }

    public Optional<PlayerRewardsProfile> getRewards(Player player) {
        return getRewards(player.getLoginChainData().getXUID());
    }

    public Optional<PlayerRewardsProfile> getRewards(String xuid) {
        return Optional.ofNullable(cache.get(xuid));
    }

    public PlayerRewardsProfile fetchRewards(Player player) throws SQLException {
        return fetchRewards(player.getLoginChainData().getXUID());
    }

    public PlayerRewardsProfile fetchRewards(String xuid) throws SQLException {
        if (cache.containsKey(xuid)) {
            return cache.get(xuid);
        }
        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        PreparedStatement stmt = null;
        try {
            stmt = wrapper.prepareStatement(new DatabaseStatement(GET_REWARDS_QUERY, new Object[]{ xuid }));
            ResultSet results = stmt.executeQuery();
            PlayerRewardsProfile record;
            boolean foundResults = results.next();
            NewGamesAPI1.get().getLogger().info(String.format("Fetched rewards from database for xuid %s and results were %s", xuid, foundResults));
            if (foundResults) {
                record = new PlayerRewardsProfile(xuid, results.getInt("experience"), results.getInt("coins"), results.getInt("tourney"));
            } else {
                record = new PlayerRewardsProfile(xuid, 0, 0, 0);
            }
            cache.put(xuid, record);
            return record;
        } finally {
            if (stmt != null) {
                DatabaseUtility.closeQuietly(stmt);
            }
            DatabaseUtility.closeQuietly(wrapper);
        }
    }

    public List<String> getCachedXUIDs() {
        return Collections.unmodifiableList(new ArrayList<>(cache.keySet()));
    }

    public void setAsPrimaryManager () {
        instance = this;
    }

    public static RewardsManager get () {
        return instance;
    }

}
