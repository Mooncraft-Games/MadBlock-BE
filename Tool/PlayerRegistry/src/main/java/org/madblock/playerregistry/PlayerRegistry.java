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
import org.madblock.util.DatabaseResult;
import org.madblock.util.DatabaseReturn;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class PlayerRegistry extends PluginBase implements Listener {

    private static PlayerRegistry pluginInst;


    private static final String CREATE_PLAYER_LOOKUP_TABLE = "CREATE TABLE IF NOT EXISTS player_lookup ( xuid VARCHAR(16) NOT NULL PRIMARY KEY, username VARCHAR(12) NOT NULL);";

    // Used for storing established links between a player xuid and an external integration.
    // Note: Discord IDs do not have a fixed length as they're the millis since jan 1st 2015! Currently, they're 17/18 chars, but I'm setting it to 28 for future-proofing.
    private static final String CREATE_PLAYER_INTEGRATION_LOOKUP_TABLE = "CREATE TABLE IF NOT EXISTS integration_links ( xuid VARCHAR(16) NOT NULL, integration VARCHAR(10) NOT NULL, identifier VARCHAR(32) NOT NULL, PRIMARY KEY (xuid, integration) );";

    // Used for linking integrations such as discord and the minecraft server.
    // service - short id - examples such as "discord", "minecraft", ... - If it's "minecraft" it can link to anything. If it's anything else, it must be linked back to minecraft unless we introduce forums.
    // identifier - user identifier - the id given to a user for that specific integration (minecraft -> [add xuid], discord -> [add discord id])
    // code - the code required on the other integration for a successful link.
    private static final String CREATE_PENDING_INTEGRATION_LINK_TABLE = "CREATE TABLE IF NOT EXISTS pending_integration_links ( integration VARCHAR(10) NOT NULL, identifier VARCHAR(32) NOT NULL, code VARCHAR(9) NOT NULL, expire BIGINT NOT NULL, PRIMARY KEY (integration, identifier) );";



    private static final String SELECT_PLAYER_QUERY = "SELECT 1 FROM player_lookup WHERE xuid=?";
    private static final String PLAYER_JOIN_QUERY = "INSERT INTO player_lookup (xuid, username) VALUES (?, ?) ON DUPLICATE KEY UPDATE username=?;";

    private static final String GET_XUID_QUERY = "SELECT xuid FROM player_lookup WHERE UPPER(username)=UPPER(?)";
    private static final String GET_USERNAME_QUERY = "SELECT username FROM player_lookup WHERE xuid=?";

    @Override
    public void onEnable() {
        pluginInst = this;

        ConnectionWrapper wrapper;

        try {
            wrapper = DatabaseAPI.getConnection("MAIN");
        } catch (SQLException connectionException) {
            this.getLogger().error("Failed to establish connection to MAIN database. Disabling...");
            this.getLogger().error(connectionException.toString());
            this.getServer().getPluginManager().disablePlugin(this);
            pluginInst = null;
            return;
        }

        PreparedStatement stmtPlayerLookup = null;
        PreparedStatement stmtTablePendingLink = null;
        PreparedStatement stmtTableLink = null;

        try {
            stmtPlayerLookup = wrapper.prepareStatement(new DatabaseStatement(CREATE_PLAYER_LOOKUP_TABLE));
            stmtPlayerLookup.execute();

            stmtTableLink = wrapper.prepareStatement(new DatabaseStatement(CREATE_PLAYER_INTEGRATION_LOOKUP_TABLE));
            stmtTableLink.execute();


            stmtTablePendingLink = wrapper.prepareStatement(new DatabaseStatement(CREATE_PENDING_INTEGRATION_LINK_TABLE));
            stmtTablePendingLink.execute();



        } catch (SQLException initException) {
            this.getLogger().error("Failed to create an essential table. Disabling...");
            this.getLogger().error(initException.toString());

            DatabaseUtility.closeQuietly(stmtPlayerLookup);
            DatabaseUtility.closeQuietly(stmtTableLink);
            DatabaseUtility.closeQuietly(stmtTablePendingLink);
            DatabaseUtility.closeQuietly(wrapper);

            this.getServer().getPluginManager().disablePlugin(this);
            pluginInst = null;
            return;
        }

        DatabaseUtility.closeQuietly(stmtPlayerLookup);
        DatabaseUtility.closeQuietly(stmtTableLink);
        DatabaseUtility.closeQuietly(stmtTablePendingLink);
        DatabaseUtility.closeQuietly(wrapper);

        this.getServer().getPluginManager().registerEvents(this, this);
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        String xuid = event.getPlayer().getLoginChainData().getXUID();
        String username = event.getPlayer().getLoginChainData().getUsername();

        getServer().getScheduler().scheduleTask(this, () -> {
            ConnectionWrapper wrapper;

            try {
                wrapper = DatabaseAPI.getConnection("MAIN");
            } catch (SQLException connectionException) {
                this.getLogger().warning("Failed to establish connection to update player registry data.");
                return;
            }

            PreparedStatement updateStmt = null;
            try {
                updateStmt = wrapper.prepareStatement(new DatabaseStatement(PLAYER_JOIN_QUERY, new Object[]{ username, xuid, username }));
                updateStmt.executeUpdate();

            } catch (SQLException updateException) {
                this.getLogger().error("An error occurred while updating player registry data.");
                this.getLogger().error(updateException.toString());
            }

            DatabaseUtility.closeQuietly(updateStmt);
            DatabaseUtility.closeQuietly(wrapper);

        }, true);
    }


    public static DatabaseReturn<String> getPlayerNameByXuid(String xuid) throws SQLException {
        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        PreparedStatement getUsernameStatement = null;
        String username = null;

        try {
            getUsernameStatement = wrapper.prepareStatement(new DatabaseStatement(GET_USERNAME_QUERY, new Object[]{ xuid }));
            ResultSet results = getUsernameStatement.executeQuery();

            if (results.next())
                username = results.getString("username");

        } catch (SQLException statementException) {
            DatabaseUtility.closeQuietly(getUsernameStatement);
            DatabaseUtility.closeQuietly(wrapper);
            throw statementException;
        }

        DatabaseUtility.closeQuietly(getUsernameStatement);
        DatabaseUtility.closeQuietly(wrapper);

        return DatabaseReturn.of(username, DatabaseResult.SUCCESS);
    }

    public static DatabaseReturn<String> getPlayerXuidByName(String username) throws SQLException {
        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        PreparedStatement getXuidStatement = null;
        String xuid = null;

        try {
            getXuidStatement = wrapper.prepareStatement(new DatabaseStatement(GET_XUID_QUERY, new Object[]{ username }));
            ResultSet results = getXuidStatement.executeQuery();

            if (results.next())
                xuid = results.getString("xuid");

        } catch (SQLException statementException) {

            DatabaseUtility.closeQuietly(getXuidStatement);
            DatabaseUtility.closeQuietly(wrapper);
            throw statementException;
        }

        DatabaseUtility.closeQuietly(getXuidStatement);
        DatabaseUtility.closeQuietly(wrapper);

        return DatabaseReturn.of(xuid, DatabaseResult.SUCCESS);
    }

    @Deprecated
    public static Optional<PlayerServerLocation> getPlayerLocationByXuid(String xuid) throws SQLException {
        return Optional.empty();
    }

    @Deprecated
    public static Optional<PlayerServerLocation> getPlayerLocationByName(String name) throws SQLException {
        return Optional.empty();
    }


    public static PlayerRegistry get() {
        return pluginInst;
    }
}