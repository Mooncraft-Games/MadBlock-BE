package org.madblock.playerregistry;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.plugin.PluginBase;
import org.madblock.database.ConnectionWrapper;
import org.madblock.database.DatabaseAPI;
import org.madblock.database.DatabaseStatement;
import org.madblock.database.DatabaseUtility;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class PlayerRegistry extends PluginBase implements Listener {

    private static final String SELECT_PLAYER_QUERY = "SELECT 1 FROM player_lookup WHERE xuid=?";
    private static final String RETURNING_PLAYER_QUERY = "UPDATE player_lookup SET username=?, server_ip=?, server_port=? WHERE xuid=?";
    private static final String NEW_PLAYER_JOIN_QUERY = "INSERT INTO player_lookup (xuid, username, server_ip, server_port) VALUES (?, ?, ?, ?)";
    private static final String PLAYER_LEAVING_QUERY = "UPDATE player_lookup SET server_ip=NULL, server_port=NULL WHERE xuid=?";
    private static final String CREATE_PLAYER_LOOKUP_TABLE = "CREATE TABLE IF NOT EXISTS player_lookup ( xuid VARCHAR(16) NOT NULL PRIMARY KEY, username VARCHAR(12) NOT NULL, server_ip VARCHAR(15), server_port INT );";
    private static final String GET_XUID_QUERY = "SELECT xuid FROM player_lookup WHERE UPPER(username)=UPPER(?)";
    private static final String GET_USERNAME_QUERY = "SELECT username FROM player_lookup WHERE xuid=?";
    private static final String GET_LOCATION_BY_NAME_QUERY = "SELECT server_ip, server_port FROM player_lookup WHERE UPPER(username)=UPPER(?)";
    private static final String GET_LOCATION_BY_XUID_QUERY = "SELECT server_ip, server_port FROM player_lookup WHERE xuid=?";

    @Override
    public void onEnable() {

        ConnectionWrapper wrapper;
        try {
            wrapper = DatabaseAPI.getConnection("MAIN");
        } catch (SQLException connectionException) {
            getLogger().error("Failed to establish connection to MAIN database. Disabling...");
            getLogger().error(connectionException.toString());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        try {
            PreparedStatement stmt = wrapper.prepareStatement(new DatabaseStatement(CREATE_PLAYER_LOOKUP_TABLE));
            stmt.execute();
        } catch (SQLException initException) {
            getLogger().error("Failed to create player_lookup table. Disabling...");
            getLogger().error(initException.toString());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getServer().getPluginManager().registerEvents(this, this);
        DatabaseUtility.closeQuietly(wrapper);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        String xuid = event.getPlayer().getLoginChainData().getXUID();
        String username = event.getPlayer().getLoginChainData().getUsername();
        String ip = getServer().getIp();
        int port = getServer().getPort();
        getServer().getScheduler().scheduleTask(this, () -> {

            ConnectionWrapper wrapper;
            try {
                wrapper = DatabaseAPI.getConnection("MAIN");
            } catch (SQLException connectionException) {
                getLogger().warning("Failed to establish connection to update player registry data.");
                return;
            }

            PreparedStatement selectPlayerStmt = null;
            boolean exists;
            try {
                selectPlayerStmt = wrapper.prepareStatement(new DatabaseStatement(SELECT_PLAYER_QUERY, new Object[]{ xuid }));
                exists = selectPlayerStmt.executeQuery().next();
            } catch (SQLException selectException) {
                getLogger().error("An error occurred while retrieving player registry data.");
                getLogger().error(selectException.toString());
                if (selectPlayerStmt != null) {
                    DatabaseUtility.closeQuietly(selectPlayerStmt);
                }
                DatabaseUtility.closeQuietly(wrapper);
                return;
            }
            DatabaseUtility.closeQuietly(selectPlayerStmt);

            PreparedStatement updateStmt = null;
            try {
                if (exists) {
                    updateStmt = wrapper.prepareStatement(new DatabaseStatement(RETURNING_PLAYER_QUERY, new Object[]{ username, ip, port, xuid}));
                } else {
                    updateStmt = wrapper.prepareStatement(new DatabaseStatement(NEW_PLAYER_JOIN_QUERY, new Object[]{ xuid, username, ip, port }));
                }
                updateStmt.executeUpdate();
            } catch (SQLException updateException) {
                getLogger().error("An error occurred while updating player registry data.");
                getLogger().error(updateException.toString());
            }
            if (updateStmt != null) {
                DatabaseUtility.closeQuietly(updateStmt);
            }
            DatabaseUtility.closeQuietly(wrapper);
        }, true);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        String xuid = event.getPlayer().getLoginChainData().getXUID();
        getServer().getScheduler().scheduleTask(this, () -> {
            ConnectionWrapper wrapper;
            try {
                wrapper = DatabaseAPI.getConnection("MAIN");
            } catch (SQLException connectionException) {
                getLogger().warning("Failed to establish connection to update player registry data.");
                return;
            }

            PreparedStatement updateStmt;
            try {
                updateStmt = wrapper.prepareStatement(new DatabaseStatement(PLAYER_LEAVING_QUERY, new Object[]{ xuid }));
                updateStmt.execute();
            } catch (SQLException exception) {
                getLogger().error("An error occurred while removing player location data");
                getLogger().error(exception.toString());
                return;
            }
            if (updateStmt != null) {
                DatabaseUtility.closeQuietly(updateStmt);
            }
            DatabaseUtility.closeQuietly(wrapper);
        }, true);
    }

    public static Optional<String> getPlayerNameByXuid(String xuid) throws SQLException {
        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        PreparedStatement getUsernameStatement = null;
        String username = null;
        try {
            getUsernameStatement = wrapper.prepareStatement(new DatabaseStatement(GET_USERNAME_QUERY, new Object[]{ xuid }));
            ResultSet results = getUsernameStatement.executeQuery();
            if (results.next()) {
                username = results.getString("username");
            }
        } catch (SQLException statementException) {
            if (getUsernameStatement != null) {
                DatabaseUtility.closeQuietly(getUsernameStatement);
            }
            DatabaseUtility.closeQuietly(wrapper);
            throw statementException;
        }
        DatabaseUtility.closeQuietly(getUsernameStatement);
        DatabaseUtility.closeQuietly(wrapper);
        if (username != null) {
            return Optional.of(username);
        }
        return Optional.empty();
    }

    public static Optional<String> getPlayerXuidByName(String username) throws SQLException {
        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        PreparedStatement getXuidStatement = null;
        String xuid = null;
        try {
            getXuidStatement = wrapper.prepareStatement(new DatabaseStatement(GET_XUID_QUERY, new Object[]{ username }));
            ResultSet results = getXuidStatement.executeQuery();
            if (results.next()) {
                xuid = results.getString("xuid");
            }
        } catch (SQLException statementException) {
            if (getXuidStatement != null) {
                DatabaseUtility.closeQuietly(getXuidStatement);
            }
            DatabaseUtility.closeQuietly(wrapper);
            throw statementException;
        }
        DatabaseUtility.closeQuietly(getXuidStatement);
        DatabaseUtility.closeQuietly(wrapper);
        if (xuid != null) {
            return Optional.of(xuid);
        }
        return Optional.empty();
    }

    public static Optional<PlayerServerLocation> getPlayerLocationByXuid(String xuid) throws SQLException {
        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        PreparedStatement getLocationByXuidStmt = null;
        PlayerServerLocation location = null;
        try {
            getLocationByXuidStmt = wrapper.prepareStatement(new DatabaseStatement(GET_LOCATION_BY_XUID_QUERY, new Object[]{ xuid }));
            ResultSet results = getLocationByXuidStmt.executeQuery();
            if (results.next() && results.getString("server_ip") != null && results.getString("server_port") != null) {
                location = new PlayerServerLocation(results.getString("server_ip"), results.getInt("server_port"));
            }
        } catch (SQLException statementException) {
            if (getLocationByXuidStmt != null) {
                DatabaseUtility.closeQuietly(getLocationByXuidStmt);
            }
            DatabaseUtility.closeQuietly(wrapper);
            throw statementException;
        }
        DatabaseUtility.closeQuietly(getLocationByXuidStmt);
        DatabaseUtility.closeQuietly(wrapper);
        return Optional.ofNullable(location);
    }

    public static Optional<PlayerServerLocation> getPlayerLocationByName(String name) throws SQLException {
        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        PreparedStatement getLocationByXuidStmt = null;
        PlayerServerLocation location = null;
        try {
            getLocationByXuidStmt = wrapper.prepareStatement(new DatabaseStatement(GET_LOCATION_BY_NAME_QUERY, new Object[]{ name }));
            ResultSet results = getLocationByXuidStmt.executeQuery();
            if (results.next() && results.getString("server_ip") != null && results.getString("server_port") != null) {
                location = new PlayerServerLocation(results.getString("server_ip"), results.getInt("server_port"));
            }
        } catch (SQLException statementException) {
            if (getLocationByXuidStmt != null) {
                DatabaseUtility.closeQuietly(getLocationByXuidStmt);
            }
            DatabaseUtility.closeQuietly(wrapper);
            throw statementException;
        }
        DatabaseUtility.closeQuietly(getLocationByXuidStmt);
        DatabaseUtility.closeQuietly(wrapper);
        return Optional.ofNullable(location);
    }

}