package org.madblock.social.friends;

import cn.nukkit.Player;
import cn.nukkit.event.Event;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.network.protocol.TransferPacket;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.utils.TextFormat;
import org.madblock.database.ConnectionWrapper;
import org.madblock.database.DatabaseAPI;
import org.madblock.database.DatabaseStatement;
import org.madblock.database.DatabaseUtility;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.playerregistry.*;
import org.madblock.social.Utility;
import org.madblock.social.events.*;
import org.madblock.social.friends.comparators.FriendComparator;
import org.madblock.social.friends.comparators.RequestsComparator;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// Honestly, SQL + multiple servers + friends is such a pain to work with. Like, what happens if someone removes you as a friend from another server?
public class FriendsManager implements Listener {

    private static FriendsManager instance;

    private static final String CREATE_FRIENDS_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS friends (" +
                                                        "player_xuid VARCHAR(16) NOT NULL," +   // Player who originally sent the friend request
                                                        "target_xuid VARCHAR(16) NOT NULL" +    // Player who accepted the friend request
                                                    ")";
    private static final String CREATE_FRIEND_REQUESTS_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS friend_requests (" +
                                                                    "from_xuid VARCHAR(16) NOT NULL," +
                                                                    "to_xuid VARCHAR(16) NOT NULL" +
                                                                ")";

    private static final String FETCH_FRIENDS_QUERY = "SELECT player_xuid AS xuid FROM friends WHERE target_xuid=? UNION SELECT target_xuid AS xuid FROM friends WHERE player_xuid=?";
    private static final String REFRESH_FRIENDS_QUERY = "SELECT player_xuid AS friend_xuid, target_xuid AS xuid FROM friends WHERE target_xuid=? UNION SELECT player_xuid AS xuid, target_xuid as friend_xuid FROM friends WHERE player_xuid=?";  // TODO: optimize this as we get duplicate rows if friendA is friends with friendB and both users are online.
    private static final String REMOVE_FRIEND_QUERY = "DELETE FROM friends WHERE (player_xuid=? AND target_xuid=?) OR (player_xuid=? AND target_xuid=?)";
    private static final String ADD_FRIEND_QUERY = "INSERT INTO friends (player_xuid, target_xuid) SELECT params.player_xuid, params.target_xuid FROM (SELECT ? AS player_xuid, ? AS target_xuid) params WHERE NOT EXISTS (SELECT 1 FROM friends WHERE (player_xuid=params.target_xuid AND target_xuid=params.player_xuid) OR (player_xuid=params.player_xuid AND target_xuid=params.target_xuid))";

    private static final String FETCH_INCOMING_FRIEND_REQUESTS_QUERY = "SELECT from_xuid AS xuid FROM friend_requests WHERE to_xuid=?";
    private static final String FETCH_OUTGOING_FRIEND_REQUESTS_QUERY = "SELECT to_xuid AS xuid FROM friend_requests WHERE from_xuid=?";
    private static final String FETCH_FRIEND_REQUESTS_QUERY = "SELECT to_xuid, from_xuid, false AS incoming FROM friend_requests WHERE from_xuid=? UNION SELECT to_xuid, from_xuid, true AS incoming FROM friend_requests WHERE to_xuid=?";
    private static final String DELETE_FRIEND_REQUEST_QUERY = "DELETE FROM friend_requests WHERE to_xuid=? AND from_xuid=?";
    private static final String SEND_FRIEND_REQUEST_QUERY = "INSERT INTO friend_requests (to_xuid, from_xuid) SELECT params.to_xuid, params.from_xuid FROM (SELECT ? AS to_xuid, ? AS from_xuid) params WHERE NOT(EXISTS((SELECT 1 FROM friend_requests WHERE (to_xuid=params.to_xuid AND from_xuid=params.from_xuid) OR (to_xuid=params.from_xuid AND from_xuid=params.to_xuid)))) AND NOT(EXISTS(SELECT 1 FROM friends WHERE ((player_xuid=params.to_xuid AND target_xuid=params.from_xuid) OR (player_xuid=params.from_xuid AND target_xuid=params.to_xuid))))";

    private final Map<String, Collection<Friend>> friendsCache = new ConcurrentHashMap<>();
    private final Map<String, FriendRequestProfile> friendRequestsCache = new ConcurrentHashMap<>();
    private final Map<Integer, FriendsFormData> formData = new HashMap<>();

    private final Plugin plugin;

    public FriendsManager (Plugin plugin) {
        this.plugin = plugin;

        ConnectionWrapper wrapper;
        try {
            wrapper = DatabaseAPI.getConnection("MAIN");
        } catch (SQLException exception) {
            plugin.getLogger().error("Unable to connect to MAIN database. Disabling.");
            plugin.getLogger().error(exception.toString());
            plugin.getPluginLoader().disablePlugin(plugin);
            return;
        }

        PreparedStatement createFriendsTableStmt = null;
        PreparedStatement createFriendRequestTableStmt = null;
        try {
            createFriendRequestTableStmt = wrapper.prepareStatement(new DatabaseStatement(CREATE_FRIEND_REQUESTS_TABLE_QUERY));
            createFriendsTableStmt = wrapper.prepareStatement(new DatabaseStatement(CREATE_FRIENDS_TABLE_QUERY));
            createFriendRequestTableStmt.execute();
            createFriendsTableStmt.execute();
        } catch (SQLException exception) {
            plugin.getLogger().error("Failed to create required tables. Disabling.");
            plugin.getLogger().error(exception.toString());
            plugin.getPluginLoader().disablePlugin(plugin);
            if (createFriendRequestTableStmt != null) {
                DatabaseUtility.closeQuietly(createFriendRequestTableStmt);
            }
            if (createFriendsTableStmt != null) {
                DatabaseUtility.closeQuietly(createFriendsTableStmt);
            }
            DatabaseUtility.closeQuietly(wrapper);
            return;
        }

        DatabaseUtility.closeQuietly(createFriendsTableStmt);
        DatabaseUtility.closeQuietly(createFriendRequestTableStmt);
        DatabaseUtility.closeQuietly(wrapper);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, this::refreshFriends,20 * 60);
        plugin.getServer().getScheduler().scheduleRepeatingTask(plugin, this::refreshFriendRequests, 20 * 60);

    }

    /**
     * Add a player as a friend
     * @param playerXuid
     * @param targetPlayerXuid
     * @return Whether or not they were added as a friend
     */
    public boolean addFriend (String playerXuid, String targetPlayerXuid) throws SQLException {
        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        PreparedStatement addFriendStmt = null;
        int rowsAffected;
        try {
            addFriendStmt = wrapper.prepareStatement(new DatabaseStatement(ADD_FRIEND_QUERY, new Object[]{ playerXuid, targetPlayerXuid }));
            rowsAffected = addFriendStmt.executeUpdate();
        } catch (SQLException exception) {
            if (addFriendStmt != null) {
                DatabaseUtility.closeQuietly(addFriendStmt);
            }
            DatabaseUtility.closeQuietly(wrapper);
            throw exception;
        }
        DatabaseUtility.closeQuietly(addFriendStmt);
        DatabaseUtility.closeQuietly(wrapper);
        if (rowsAffected == 0) {
            return false;
        }
        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
            Optional<String> name;
            try {
                name = PlayerRegistry.getPlayerNameByXuid(playerXuid);
            } catch (SQLException exception) {
                plugin.getLogger().error(String.format("Failed to retrieve name of xuid when running addFriend: %s", playerXuid));
                plugin.getLogger().error(exception.toString());
                return;
            }
            if (name.isPresent()) {
                Friend playerFriend = new Friend(playerXuid, name.get());
                synchronized (friendsCache) {
                    if (friendsCache.containsKey(targetPlayerXuid)) {
                        friendsCache.get(targetPlayerXuid).add(playerFriend);
                    } else {
                        Collection<Friend> friends = new HashSet<>();
                        friends.add(playerFriend);
                        friendsCache.put(targetPlayerXuid, friends);
                    }
                }
                plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                    Optional<Player> player = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(targetPlayerXuid)).findAny();
                    if (player.isPresent()) {
                        PlayerObtainedFriendEvent event = new PlayerObtainedFriendEvent(player.get(), playerFriend);
                        plugin.getServer().getPluginManager().callEvent(event);
                    }
                });
            }

            Optional<String> targetName;
            try {
                targetName = PlayerRegistry.getPlayerNameByXuid(targetPlayerXuid);
            } catch (SQLException exception) {
                plugin.getLogger().error(String.format("Failed to retrieve name of xuid when running addFriend: %s", targetPlayerXuid));
                plugin.getLogger().error(exception.toString());
                return;
            }
            if (targetName.isPresent()) {
                Friend targetFriend = new Friend(targetPlayerXuid, targetName.get());
                synchronized (friendsCache) {
                    if (friendsCache.containsKey(playerXuid)) {
                        friendsCache.get(playerXuid).add(targetFriend);
                    } else {
                        Collection<Friend> friends = new HashSet<>();
                        friends.add(targetFriend);
                        friendsCache.put(playerXuid, friends);
                    }
                }
                plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                    Optional<Player> player = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(playerXuid)).findAny();
                    if (player.isPresent()) {
                        PlayerObtainedFriendEvent event = new PlayerObtainedFriendEvent(player.get(), targetFriend);
                        plugin.getServer().getPluginManager().callEvent(event);
                    }
                });
            }
        }, true);
        return true;
    }

    /**
     * Remove a player's friend
     * @param playerXuid
     * @param targetPlayerXuid
     * @return Whether or not the player was removed as a friend
     */
    public boolean removeFriend (String playerXuid, String targetPlayerXuid) throws SQLException {
        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        PreparedStatement removeFriendStmt = null;
        boolean deleted;
        try {
            removeFriendStmt = wrapper.prepareStatement(new DatabaseStatement(REMOVE_FRIEND_QUERY, new Object[]{ playerXuid, targetPlayerXuid, targetPlayerXuid, playerXuid }));
            deleted = removeFriendStmt.executeUpdate() > 0;
        } finally {
            if (removeFriendStmt != null) {
                DatabaseUtility.closeQuietly(removeFriendStmt);
            }
            DatabaseUtility.closeQuietly(wrapper);
        }

        if (deleted) {
            plugin.getServer().getScheduler().scheduleTask(plugin, () -> {

                Optional<String> name;
                try {
                    name = PlayerRegistry.getPlayerNameByXuid(playerXuid);
                } catch (SQLException exception) {
                    plugin.getLogger().error(String.format("Failed to retrieve name of xuid when running removeFriend: %s", playerXuid));
                    plugin.getLogger().error(exception.toString());
                    return;
                }

                if (name.isPresent()) {
                    Friend friend = new Friend(playerXuid, name.get());

                    friendsCache.computeIfPresent(targetPlayerXuid, (k, friends) -> {
                        friends.remove(friend);
                        return friends;
                    });

                    plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                        Optional<Player> player = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(targetPlayerXuid)).findAny();
                        if (player.isPresent()) {
                            PlayerLostFriendEvent event = new PlayerLostFriendEvent(player.get(), friend);
                            plugin.getServer().getPluginManager().callEvent(event);
                        }
                    });
                }

                Optional<String> targetName;
                try {
                    targetName = PlayerRegistry.getPlayerNameByXuid(targetPlayerXuid);
                } catch (SQLException exception) {
                    plugin.getLogger().error(String.format("Failed to retrieve name of xuid when running removeFriend: %s", targetPlayerXuid));
                    plugin.getLogger().error(exception.toString());
                    return;
                }

                if (targetName.isPresent()) {
                    Friend friend = new Friend(targetPlayerXuid, targetName.get());

                    friendsCache.computeIfPresent(playerXuid, (k, friends) -> {
                        friends.remove(friend);
                        return friends;
                    });

                    plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                        Optional<Player> player = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(playerXuid)).findAny();
                        if (player.isPresent()) {
                            PlayerLostFriendEvent event = new PlayerLostFriendEvent(player.get(), friend);
                            plugin.getServer().getPluginManager().callEvent(event);
                        }
                    });
                }
            }, true);
        }

        return deleted;
    }

    /**
     * Send a friend request to another player
     * @param playerXuid
     * @param targetPlayerXuid
     * @return whether or not the friend request went through
     */
    public boolean sendFriendRequest (String playerXuid, String targetPlayerXuid) throws SQLException {
        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        PreparedStatement sendFriendRequestStmt = null;
        try {
            sendFriendRequestStmt = wrapper.prepareStatement(new DatabaseStatement(SEND_FRIEND_REQUEST_QUERY, new Object[]{ targetPlayerXuid, playerXuid }));
            int rows = sendFriendRequestStmt.executeUpdate();
            if (rows == 0) {
                return false;
            }
        } finally {
            if (sendFriendRequestStmt != null) {
                DatabaseUtility.closeQuietly(sendFriendRequestStmt);
            }
            DatabaseUtility.closeQuietly(wrapper);
        }

        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
            Optional<String> playerName;
            try {
                playerName = PlayerRegistry.getPlayerNameByXuid(playerXuid);
            } catch (SQLException nameSqlException) {
                plugin.getLogger().error(String.format("Failed to retrieve name of xuid when running sendFriendRequest: %s", playerXuid));
                plugin.getLogger().error(nameSqlException.toString());
                return;
            }
            if (playerName.isPresent()) {
                FriendRequest targetIncomingRequest = new FriendRequest(playerXuid, playerName.get());
                synchronized (friendRequestsCache) {
                    if (friendRequestsCache.containsKey(targetPlayerXuid)) {
                        friendRequestsCache.get(targetPlayerXuid).addIncomingFriendRequest(targetIncomingRequest);
                    } else {
                        FriendRequestProfile profile = new FriendRequestProfile();
                        profile.addIncomingFriendRequest(targetIncomingRequest);
                        friendRequestsCache.put(targetPlayerXuid, profile);
                    }
                }
                plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                    Optional<Player> target = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(targetPlayerXuid)).findAny();
                    if (target.isPresent()) {
                        PlayerObtainedFriendRequestEvent event = new PlayerObtainedFriendRequestEvent(target.get(), targetIncomingRequest);
                        plugin.getServer().getPluginManager().callEvent(event);
                    }
                });
            }
            Optional<String> targetPlayerName;
            try {
                targetPlayerName = PlayerRegistry.getPlayerNameByXuid(targetPlayerXuid);
            } catch (SQLException nameSqlException) {
                plugin.getLogger().error(String.format("Failed to retrieve name of xuid when running sendFriendRequest: %s", targetPlayerXuid));
                plugin.getLogger().error(nameSqlException.toString());
                return;
            }

            if (!targetPlayerName.isPresent()) {
                plugin.getLogger().error(String.format("Xuid has no name while running sendFriendRequest: %s", targetPlayerXuid));
                return;
            }

            FriendRequest playerOutgoingRequest = new FriendRequest(targetPlayerXuid, targetPlayerName.get());
            synchronized (friendRequestsCache) {
                if (friendRequestsCache.containsKey(playerXuid)) {
                    friendRequestsCache.get(playerXuid).addOutgoingFriendRequest(playerOutgoingRequest);
                } else {
                    FriendRequestProfile profile = new FriendRequestProfile();
                    profile.addOutgoingFriendRequest(playerOutgoingRequest);
                    friendRequestsCache.put(playerXuid, profile);
                }
            }
        }, true);
        return true;
    }

    /**
     * Reject a friend request
     * @param playerXuid The player who got the friend request.
     * @param targetXuid The player who sent the friend request.
     * @return if the request was rejected
     */
    public boolean rejectFriendRequest (String playerXuid, String targetXuid) throws SQLException {
        boolean deleted = deleteFriendRequest(targetXuid, playerXuid);
        if (deleted) {
            plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                Optional<FriendRequest> outgoingRequest = friendRequestsCache.getOrDefault(targetXuid, new FriendRequestProfile()).getIncomingRequests().stream().filter(request -> request.getXuid().equals(playerXuid)).findAny();
                Optional<Player> player = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(targetXuid)).findAny();
                if (player.isPresent() && outgoingRequest.isPresent()) {
                    FriendRequestRejectedEvent event = new FriendRequestRejectedEvent(player.get(), outgoingRequest.get());
                    plugin.getServer().getPluginManager().callEvent(event);
                }
            });
        }
        return deleted;
    }

    /**
     * Accept a friend request
     * @param playerXuid
     * @param targetXuid
     * @return if the request was accepted
     */
    public boolean acceptFriendRequest (String playerXuid, String targetXuid) throws SQLException {
        boolean deleted = deleteFriendRequest(targetXuid, playerXuid);
        if (!deleted) {
            return false;
        }
        return addFriend(playerXuid, targetXuid);
    }

    /**
     * Cancel a friend request
     * @param playerXuid
     * @param targetXuid
     * @return if the request was cancelled.
     */
    public boolean cancelFriendRequest (String playerXuid, String targetXuid) throws SQLException {
        Optional<FriendRequest> incomingRequest = friendRequestsCache.getOrDefault(targetXuid, new FriendRequestProfile()).getIncomingRequests().stream().filter(request -> request.getXuid().equals(playerXuid)).findAny();  // The target should have this request
        Optional<FriendRequest> outgoingRequest = friendRequestsCache.getOrDefault(playerXuid, new FriendRequestProfile()).getOutgoingRequests().stream().filter(request -> request.getXuid().equals(targetXuid)).findAny();  // The player should have this request.

        boolean deleted = deleteFriendRequest(playerXuid, targetXuid);
        if (deleted) {
            plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                Optional<Player> sender = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(playerXuid)).findAny();
                Optional<Player> target = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(targetXuid)).findAny();
                if (sender.isPresent() && outgoingRequest.isPresent()) {
                    OutgoingFriendRequestCancelledEvent event = new OutgoingFriendRequestCancelledEvent(sender.get(), outgoingRequest.get());
                    plugin.getServer().getPluginManager().callEvent(event);
                }
                if (target.isPresent() && incomingRequest.isPresent()) {
                    IncomingFriendRequestCancelledEvent event = new IncomingFriendRequestCancelledEvent(target.get(), incomingRequest.get());
                    plugin.getServer().getPluginManager().callEvent(event);
                }
            });
        }
        return deleted;

    }

    /**
     * Get all the friends of a player
     * @param playerXuid
     * @return a collection of friends
     */
    public Collection<Friend> getFriends (String playerXuid) throws SQLException {
        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        Collection<Friend> friends = new HashSet<>();

        PreparedStatement fetchFriendsQueryStmt = null;
        try {
            fetchFriendsQueryStmt = wrapper.prepareStatement(new DatabaseStatement(FETCH_FRIENDS_QUERY, new Object[]{ playerXuid, playerXuid }));
            ResultSet results = fetchFriendsQueryStmt.executeQuery();
            while (results.next()) {
                // For each friend, construct the friend object and add it to the cache it's missing.
                String friendXuid = results.getString("xuid");
                Optional<String> name = PlayerRegistry.getPlayerNameByXuid(friendXuid);
                Optional<PlayerServerLocation> location = PlayerRegistry.getPlayerLocationByXuid(friendXuid);
                if (!name.isPresent()) {
                    continue;   // Name isn't registered in database?...
                }
                Friend friend = new Friend(friendXuid, name.get(), location);
                friends.add(friend);
                synchronized (friendsCache) {
                    if (friendsCache.containsKey(playerXuid)) {
                        // Did we gain any new friends while we were online?
                        boolean storedInCache = friendsCache.get(playerXuid).contains(friend);
                        if (!storedInCache) {

                            friendsCache.get(playerXuid).add(friend);
                            plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                                Optional<Player> player = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(playerXuid)).findAny();
                                if (player.isPresent()) {
                                    PlayerObtainedFriendEvent event = new PlayerObtainedFriendEvent(player.get(), friend);
                                    plugin.getServer().getPluginManager().callEvent(event);
                                }
                            });
                        } else {
                            // Did their status change?
                            Friend oldFriendObj = friendsCache.get(playerXuid).stream().filter(f -> f.getXuid().equals(friend.getXuid())).findAny().get();
                            if (oldFriendObj.isOnline() != friend.isOnline()) {
                                oldFriendObj.setLocation(friend.getLocation());
                                plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                                    Optional<Player> player = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(playerXuid)).findAny();
                                    if (player.isPresent()) {
                                        Event event;
                                        if (friend.isOnline()) {
                                            event = new FriendOnlineEvent(player.get(), oldFriendObj);
                                        } else {
                                            event = new FriendOfflineEvent(player.get(), oldFriendObj);
                                        }
                                        plugin.getServer().getPluginManager().callEvent(event);
                                    }
                                });
                            }
                        }
                    } else {
                        Collection<Friend> data = new HashSet<>();
                        data.add(friend);
                        friendsCache.put(playerXuid, data);
                        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                            Optional<Player> player = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(playerXuid)).findAny();
                            if (player.isPresent()) {
                                PlayerObtainedFriendEvent event = new PlayerObtainedFriendEvent(player.get(), friend);
                                plugin.getServer().getPluginManager().callEvent(event);
                            }
                        });
                    }
                }
            }
        } finally {
            if (fetchFriendsQueryStmt != null) {
                DatabaseUtility.closeQuietly(fetchFriendsQueryStmt);
            }
            DatabaseUtility.closeQuietly(wrapper);
        }
        return friends;
    }

    /**
     * Get all incoming friend requests for a player
     * @param playerXuid
     * @return a collection of incoming friend requests
     */
    public Collection<FriendRequest> getIncomingFriendRequests (String playerXuid) throws SQLException {
        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        // We keep a copy of the old friend requests so we can compare and contrast which friends were removed.
        Collection<FriendRequest> removedFriendRequests = new HashSet<>(friendRequestsCache.getOrDefault(playerXuid, new FriendRequestProfile()).getIncomingRequests());

        PreparedStatement fetchIncomingFriendRequestsQuery = null;
        try {
            fetchIncomingFriendRequestsQuery = wrapper.prepareStatement(new DatabaseStatement(FETCH_INCOMING_FRIEND_REQUESTS_QUERY, new Object[]{ playerXuid }));
            ResultSet results = fetchIncomingFriendRequestsQuery.executeQuery();
            while (results.next()) {
                String xuid = results.getString("xuid");
                Optional<String> name = PlayerRegistry.getPlayerNameByXuid(xuid);
                if (name.isPresent()) {
                    FriendRequest request = new FriendRequest(xuid, name.get());
                    FriendRequestProfile profile = friendRequestsCache.getOrDefault(playerXuid, new FriendRequestProfile());
                    removedFriendRequests.remove(request);
                    if (!profile.getIncomingRequests().contains(request)) {
                        profile.addIncomingFriendRequest(request);
                        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {   // Call the event
                            Optional<Player> player = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(playerXuid)).findAny();
                            if (player.isPresent()) {
                                PlayerObtainedFriendRequestEvent event = new PlayerObtainedFriendRequestEvent(player.get(), request);
                                plugin.getServer().getPluginManager().callEvent(event);
                            }
                        });
                        friendRequestsCache.put(playerXuid, profile);
                    }
                }
            }
        } finally {
            if (fetchIncomingFriendRequestsQuery != null) {
                DatabaseUtility.closeQuietly(fetchIncomingFriendRequestsQuery);
            }
            DatabaseUtility.closeQuietly(wrapper);
        }

        FriendRequestProfile profile = friendRequestsCache.getOrDefault(playerXuid, new FriendRequestProfile());
        for (FriendRequest request : removedFriendRequests) {
            profile.removeIncomingFriendRequest(request);
            synchronized (friendsCache) {
                if (!friendsCache.containsKey(playerXuid) || !friendsCache.get(playerXuid).contains(new Friend(request.getXuid(), request.getUsername()))) {
                    plugin.getServer().getScheduler().scheduleTask(plugin, () -> {   // Call the event
                        Optional<Player> player = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(playerXuid)).findAny();
                        if (player.isPresent()) {
                            IncomingFriendRequestCancelledEvent event = new IncomingFriendRequestCancelledEvent(player.get(), request);
                            plugin.getServer().getPluginManager().callEvent(event);
                        }
                    });
                }
            }
        }

        return profile.getIncomingRequests();
    }

    /**
     * Get all outgoing friend requests for a player
     * @param playerXuid
     * @return a collection of outgoing friend requests
     */
    public Collection<FriendRequest> getOutgoingFriendRequests (String playerXuid) throws SQLException {
        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        // We keep a copy of the old friend requests so we can compare and contrast which friends were removed.
        Collection<FriendRequest> removedRequests = new HashSet<>(friendRequestsCache.getOrDefault(playerXuid, new FriendRequestProfile()).getOutgoingRequests());
        PreparedStatement fetchOutgoingFriendRequestsQuery = null;
        try {
            fetchOutgoingFriendRequestsQuery = wrapper.prepareStatement(new DatabaseStatement(FETCH_OUTGOING_FRIEND_REQUESTS_QUERY, new Object[]{ playerXuid }));
            ResultSet results = fetchOutgoingFriendRequestsQuery.executeQuery();
            while (results.next()) {
                String xuid = results.getString("xuid");
                Optional<String> name = PlayerRegistry.getPlayerNameByXuid(xuid);
                PlayerRegistry.getPlayerNameByXuid(xuid);
                if (name.isPresent()) {
                    FriendRequest request = new FriendRequest(xuid, name.get());
                    removedRequests.remove(request);
                    FriendRequestProfile profile = friendRequestsCache.getOrDefault(playerXuid, new FriendRequestProfile());
                    if (!profile.getOutgoingRequests().contains(request)) {
                        profile.addOutgoingFriendRequest(request);
                        friendRequestsCache.putIfAbsent(playerXuid, profile);
                        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {   // Call the event
                            Optional<Player> player = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(playerXuid)).findAny();
                            if (player.isPresent()) {
                                PlayerObtainedFriendRequestEvent event = new PlayerObtainedFriendRequestEvent(player.get(), request);
                                plugin.getServer().getPluginManager().callEvent(event);
                            }
                        });
                    }
                }
            }
        } finally {
            if (fetchOutgoingFriendRequestsQuery != null) {
                DatabaseUtility.closeQuietly(fetchOutgoingFriendRequestsQuery);
            }
            DatabaseUtility.closeQuietly(wrapper);
        }

        FriendRequestProfile profile = friendRequestsCache.getOrDefault(playerXuid, new FriendRequestProfile());
        synchronized (friendRequestsCache) {
            if (friendRequestsCache.containsKey(playerXuid)) {
                for (FriendRequest request : removedRequests) {
                    profile.removeOutgoingFriendRequest(request);
                    plugin.getServer().getScheduler().scheduleTask(plugin, () -> {   // Call the event
                        Optional<Player> player = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(playerXuid)).findAny();
                        if (player.isPresent()) {
                            FriendRequestRejectedEvent event = new FriendRequestRejectedEvent(player.get(), request);
                            plugin.getServer().getPluginManager().callEvent(event);
                        }
                    });
                }
            }
        }
        return profile.getOutgoingRequests();
    }

    /**
     * Update friend requests for all players currently online with data from the database.
     * This is a async task
     */
    public void refreshFriendRequests () {
        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
            StringBuilder queryBuilder = new StringBuilder(FETCH_FRIEND_REQUESTS_QUERY);
            Iterator<Player> onlinePlayers = plugin.getServer().getOnlinePlayers().values().iterator();
            List<String> sqlParamsList = new ArrayList<>();
            if (!onlinePlayers.hasNext()) {
                return;
            }
            String firstPlayerXuid = onlinePlayers.next().getLoginChainData().getXUID();
            sqlParamsList.add(firstPlayerXuid);
            sqlParamsList.add(firstPlayerXuid);
            while (onlinePlayers.hasNext()) {
                String playerXuid = onlinePlayers.next().getLoginChainData().getXUID();
                sqlParamsList.add(playerXuid);
                sqlParamsList.add(playerXuid);
                queryBuilder.append(" UNION ").append(FETCH_FRIEND_REQUESTS_QUERY);
            }
            ConnectionWrapper wrapper;
            try {
                wrapper = DatabaseAPI.getConnection("MAIN");
            } catch (SQLException exception) {
                plugin.getLogger().error("Unable to connect to MAIN database while trying to run refreshFriendRequests.");
                plugin.getLogger().error(exception.toString());
                return;
            }

            PreparedStatement fetchFriendRequestsQuery = null;
            try {
                Map<String, FriendRequestProfile> removedFriendRequests = new HashMap<>();
                fetchFriendRequestsQuery = wrapper.prepareStatement(new DatabaseStatement(queryBuilder.toString(), sqlParamsList.toArray()));
                ResultSet results = fetchFriendRequestsQuery.executeQuery();
                synchronized (friendRequestsCache) {

                    // First we need to make sure that we can figure out which friend requests were removed.
                    // The idea is to save all current incoming friend requests before changing any data.
                    // As we go through each incoming friend request from the database, remove the friend request from the saved map we created.
                    // and we add it to our cache if it's not in there already.
                    // At the end, we now have a map of all friend requests that should be removed.
                    // Additionally, any request that is not in our cache is a new friend request.

                    for (String xuid : friendRequestsCache.keySet()) {
                        removedFriendRequests.put(xuid, new FriendRequestProfile(friendRequestsCache.get(xuid)));
                    }

                    while (results.next()) {
                        String toXuid = results.getString("to_xuid");
                        String fromXuid = results.getString("from_xuid");
                        boolean isIncoming = results.getBoolean("incoming");

                        String profileXuid = isIncoming ? toXuid : fromXuid;
                        String friendRequestXuid = isIncoming ? fromXuid : toXuid;

                        Optional<String> name = PlayerRegistry.getPlayerNameByXuid(friendRequestXuid);
                        if (name.isPresent()) {
                            FriendRequest request = new FriendRequest(friendRequestXuid, name.get());
                            if (removedFriendRequests.containsKey(profileXuid)) {
                                if (!isIncoming && removedFriendRequests.get(profileXuid).getOutgoingRequests().contains(request)) {
                                    removedFriendRequests.get(profileXuid).removeOutgoingFriendRequest(request);
                                } else if (isIncoming && removedFriendRequests.get(profileXuid).getIncomingRequests().contains(request)) {
                                    removedFriendRequests.get(profileXuid).removeIncomingFriendRequest(request);
                                }
                            }
                            if (!friendRequestsCache.containsKey(profileXuid)) {
                                FriendRequestProfile profile = new FriendRequestProfile();
                                if (isIncoming) {
                                    profile.addIncomingFriendRequest(request);
                                } else {
                                    profile.addOutgoingFriendRequest(request);
                                }
                                friendRequestsCache.put(profileXuid, profile);
                                if (isIncoming) {
                                    plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                                        Optional<Player> player = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(profileXuid)).findAny();
                                        if (player.isPresent()) {
                                            PlayerObtainedFriendRequestEvent event = new PlayerObtainedFriendRequestEvent(player.get(), request);
                                            plugin.getServer().getPluginManager().callEvent(event);
                                        }
                                    });
                                }
                            } else if (isIncoming && !friendRequestsCache.get(profileXuid).getIncomingRequests().contains(request)) {
                                friendRequestsCache.get(profileXuid).addIncomingFriendRequest(request);
                                plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                                    Optional<Player> player = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(profileXuid)).findAny();
                                    if (player.isPresent()) {
                                        PlayerObtainedFriendRequestEvent event = new PlayerObtainedFriendRequestEvent(player.get(), request);
                                        plugin.getServer().getPluginManager().callEvent(event);
                                    }
                                });
                            } else if (!isIncoming && !friendRequestsCache.get(profileXuid).getOutgoingRequests().contains(request)) {
                                friendRequestsCache.get(profileXuid).addOutgoingFriendRequest(request);
                            }
                        } else {
                            plugin.getLogger().warning(String.format("Failed to retrieve the username for the xuid %s", friendRequestXuid));
                        }
                    }

                    // Get rid of requests that are no longer valid.
                    for (String xuid : removedFriendRequests.keySet()) {
                        FriendRequestProfile profile = removedFriendRequests.get(xuid);
                        for (FriendRequest request : profile.getIncomingRequests()) {
                            if (friendRequestsCache.containsKey(xuid)) {
                                friendRequestsCache.get(xuid).removeIncomingFriendRequest(request);
                            }
                            Optional<String> name = PlayerRegistry.getPlayerNameByXuid(xuid);
                            if (!name.isPresent()) {
                                plugin.getLogger().warning(String.format("Failed to retrieve the username for the xuid %s", xuid));
                                continue;
                            }
                            synchronized (friendsCache) {
                                if (!friendsCache.containsKey(xuid) || !friendsCache.get(xuid).contains(new Friend(request.getXuid(), request.getUsername()))) {
                                    plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                                        Optional<Player> player = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(xuid)).findAny();
                                        if (player.isPresent()) {
                                            IncomingFriendRequestCancelledEvent event = new IncomingFriendRequestCancelledEvent(player.get(), request);
                                            plugin.getServer().getPluginManager().callEvent(event);
                                        }
                                    });
                                }
                            }
                        }

                        for (FriendRequest request : profile.getOutgoingRequests()) {
                            if (friendRequestsCache.containsKey(xuid)) {
                                friendRequestsCache.get(xuid).removeOutgoingFriendRequest(request);
                                synchronized (friendsCache) {
                                    if (!friendsCache.containsKey(xuid) || !friendsCache.get(xuid).contains(new Friend(request.getXuid(), request.getUsername()))) {
                                        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                                            Optional<Player> player = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(xuid)).findAny();
                                            if (player.isPresent()) {
                                                FriendRequestRejectedEvent event = new FriendRequestRejectedEvent(player.get(), request);
                                                plugin.getServer().getPluginManager().callEvent(event);
                                            }
                                        });
                                    }
                                }
                            }
                        }

                    }

                }

            } catch (SQLException exception) {
                plugin.getLogger().error("Failed to refresh friend requests data when running refreshFriendRequests");
                plugin.getLogger().error(exception.toString());
            } finally {
                if (fetchFriendRequestsQuery != null) {
                    DatabaseUtility.closeQuietly(fetchFriendRequestsQuery);
                }
                DatabaseUtility.closeQuietly(wrapper);
            }

        }, true);
    }

    /**
     * Update friends for all players currently online with data from the database.
     * This is a async task.
     */
    public void refreshFriends () { // TODO: this function is way too long. Look into this again when you have the time.
        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
            StringBuilder queryBuilder = new StringBuilder(REFRESH_FRIENDS_QUERY);
            Iterator<Player> onlinePlayers = plugin.getServer().getOnlinePlayers().values().iterator();
            List<String> sqlParamsList = new ArrayList<>();
            if (!onlinePlayers.hasNext()) {
                return;
            }
            String firstPlayerXuid = onlinePlayers.next().getLoginChainData().getXUID();
            sqlParamsList.add(firstPlayerXuid);
            sqlParamsList.add(firstPlayerXuid);
            while (onlinePlayers.hasNext()) {
                String playerXuid = onlinePlayers.next().getLoginChainData().getXUID();
                sqlParamsList.add(playerXuid);
                sqlParamsList.add(playerXuid);
                queryBuilder.append(" UNION ").append(REFRESH_FRIENDS_QUERY);
            }

            ConnectionWrapper wrapper;
            try {
                wrapper = DatabaseAPI.getConnection("MAIN");
            } catch (SQLException exception) {
                plugin.getLogger().error("Unable to connect to MAIN database while trying to refreshFriends.");
                plugin.getLogger().error(exception.toString());
                return;
            }

            Map<String, Collection<Friend>> currentFriends = new HashMap<>(); // Stores current data to be used to determine which friends were removed.
            PreparedStatement refreshFriendsQueryStmt = null;
            try {
                refreshFriendsQueryStmt = wrapper.prepareStatement(new DatabaseStatement(queryBuilder.toString(), sqlParamsList.toArray()));
                ResultSet results = refreshFriendsQueryStmt.executeQuery();
                while (results.next()) {

                    String friendXuid = results.getString("friend_xuid");
                    String xuid = results.getString("xuid");

                    Optional<String> name = PlayerRegistry.getPlayerNameByXuid(friendXuid);
                    if (!name.isPresent()) {
                        plugin.getLogger().warning(String.format("Failed to retrieve the username for the xuid %s", friendXuid));
                        return;
                    }

                    Optional<PlayerServerLocation> location = PlayerRegistry.getPlayerLocationByXuid(friendXuid);
                    Friend friend = new Friend(friendXuid, name.get(), location);

                    currentFriends.putIfAbsent(xuid, new HashSet<>());
                    currentFriends.get(xuid).add(friend);

                    synchronized (friendsCache) {

                        if (friendsCache.containsKey(xuid)) {
                            // Is this friend someone new or old?
                            if (friendsCache.get(xuid).contains(friend)) {
                                // Did their status change?
                                Friend oldFriendObj = friendsCache.get(xuid).stream().filter(f -> f.getXuid().equals(friend.getXuid())).findAny().get();
                                if (oldFriendObj.isOnline() != friend.isOnline()) {
                                    oldFriendObj.setLocation(friend.getLocation());
                                    plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                                        Optional<Player> player = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(xuid)).findAny();
                                        if (player.isPresent()) {
                                            Event event;
                                            if (friend.isOnline()) {
                                                event = new FriendOnlineEvent(player.get(), oldFriendObj);
                                            } else {
                                                event = new FriendOfflineEvent(player.get(), oldFriendObj);
                                            }
                                            plugin.getServer().getPluginManager().callEvent(event);
                                        }
                                    });
                                }
                            } else {
                                // New friend!
                                friendsCache.get(xuid).add(friend);
                                plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                                    Optional<Player> player = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(xuid)).findAny();
                                    if (player.isPresent()) {
                                        PlayerObtainedFriendEvent event = new PlayerObtainedFriendEvent(player.get(), friend);
                                        plugin.getServer().getPluginManager().callEvent(event);
                                    }
                                });
                            }
                        } else {
                            // This is a new friend no matter what!
                            friendsCache.put(xuid, new HashSet<>(currentFriends.get(xuid)));
                            plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                                Optional<Player> player = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(xuid)).findAny();
                                if (player.isPresent()) {
                                    PlayerObtainedFriendEvent event = new PlayerObtainedFriendEvent(player.get(), friend);
                                    plugin.getServer().getPluginManager().callEvent(event);
                                }
                            });
                        }
                    }
                }
            } catch (SQLException exception) {
                plugin.getLogger().error("Failed to read friends data while running refreshFriends");
                plugin.getLogger().error(exception.toString());
                return;
            } finally {
                if (refreshFriendsQueryStmt != null) {
                    DatabaseUtility.closeQuietly(refreshFriendsQueryStmt);
                }
                DatabaseUtility.closeQuietly(wrapper);
            }

            plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                // Go through friendsCache and for any item not in currentFriends, that is a removed friend.
                synchronized (friendsCache) {

                    // Update any existing keys
                    Iterator<String> xuidKeyIterator = friendsCache.keySet().iterator();
                    while (xuidKeyIterator.hasNext()) {
                        String xuid = xuidKeyIterator.next();

                        if (!currentFriends.containsKey(xuid)) {
                            // Remove ALL friends there and the key
                            for (Friend friend : friendsCache.get(xuid)) {
                                Optional<Player> player = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(xuid)).findAny();
                                if (player.isPresent()) {
                                    PlayerLostFriendEvent event = new PlayerLostFriendEvent(player.get(), friend);
                                    plugin.getServer().getPluginManager().callEvent(event);
                                }
                            }
                            xuidKeyIterator.remove();
                            continue;
                        }

                        for (Friend friend : friendsCache.get(xuid)) {
                            if (!currentFriends.get(xuid).contains(friend)) {
                                // Friend removed
                                Optional<Player> player = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(xuid)).findAny();
                                if (player.isPresent()) {
                                    PlayerLostFriendEvent event = new PlayerLostFriendEvent(player.get(), friend);
                                    plugin.getServer().getPluginManager().callEvent(event);
                                }
                            }
                        }

                    }

                }
            });

        }, true);
    }

    /**
     * Load friend data for a player
     * @param player
     */
    public void loadFriendData (Player player) { // TODO: this function is way too long. Look into this again when you have the time.
        String xuid = player.getLoginChainData().getXUID();
        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
            ConnectionWrapper wrapper;
            try {
                wrapper = DatabaseAPI.getConnection("MAIN");
            } catch (SQLException exception) {
                plugin.getLogger().error("Failed to connect to MAIN database while loading friends for a player");
                plugin.getLogger().error(exception.toString());
                return;
            }

            // First, fetch friends.
            PreparedStatement fetchFriendsQueryStmt = null;
            try {
                Collection<Friend> friends = new HashSet<>();
                fetchFriendsQueryStmt = wrapper.prepareStatement(new DatabaseStatement(FETCH_FRIENDS_QUERY, new Object[]{ xuid, xuid }));
                ResultSet results = fetchFriendsQueryStmt.executeQuery();
                while (results.next()) {
                    String targetXuid = results.getString("xuid");
                    Optional<String> name = PlayerRegistry.getPlayerNameByXuid(targetXuid);
                    if (!name.isPresent()) {
                        plugin.getLogger().warning(String.format("Failed to retrieve the username for the xuid %s", targetXuid));
                        continue;
                    }
                    Optional<PlayerServerLocation> location = PlayerRegistry.getPlayerLocationByXuid(targetXuid);
                    friends.add(new Friend(targetXuid, name.get(), location));
                }
                friendsCache.put(xuid, friends);
            } catch (SQLException exception) {
                plugin.getLogger().error(String.format("Failed to load friend data for %s while running loadFriendData", player.getName()));
                plugin.getLogger().error(exception.toString());
                DatabaseUtility.closeQuietly(wrapper);
                return;
            } finally {
                if (fetchFriendsQueryStmt != null) {
                    DatabaseUtility.closeQuietly(fetchFriendsQueryStmt);
                }
            }

            FriendRequestProfile profile = new FriendRequestProfile();

            // Fetch incoming requests
            PreparedStatement fetchIncomingFriendRequestsQueryStmt = null;
            try {
                fetchIncomingFriendRequestsQueryStmt = wrapper.prepareStatement(new DatabaseStatement(FETCH_INCOMING_FRIEND_REQUESTS_QUERY, new Object[]{ xuid }));
                ResultSet results = fetchIncomingFriendRequestsQueryStmt.executeQuery();
                while (results.next()) {
                    String targetXuid = results.getString("xuid");
                    Optional<String> targetName = PlayerRegistry.getPlayerNameByXuid(targetXuid);
                    if (!targetName.isPresent()) {
                        plugin.getLogger().warning(String.format("Failed to retrieve the username for the xuid %s", targetXuid));
                        continue;
                    }
                    FriendRequest request = new FriendRequest(targetXuid, targetName.get());
                    profile.addIncomingFriendRequest(request);
                }
            } catch (SQLException exception) {
                plugin.getLogger().error(String.format("Failed to load incoming friend request data for %s while running loadFriendData", player.getName()));
                plugin.getLogger().error(exception.toString());
                DatabaseUtility.closeQuietly(wrapper);
                return;
            } finally {
                if (fetchIncomingFriendRequestsQueryStmt != null) {
                    DatabaseUtility.closeQuietly(fetchIncomingFriendRequestsQueryStmt);
                }
            }

            // Fetch outgoing requests
            PreparedStatement fetchOutgoingFriendRequestsQueryStmt = null;
            try {
                fetchOutgoingFriendRequestsQueryStmt = wrapper.prepareStatement(new DatabaseStatement(FETCH_OUTGOING_FRIEND_REQUESTS_QUERY, new Object[]{ xuid }));
                ResultSet results = fetchOutgoingFriendRequestsQueryStmt.executeQuery();
                while (results.next()) {
                    String targetXuid = results.getString("xuid");
                    Optional<String> targetName = PlayerRegistry.getPlayerNameByXuid(targetXuid);
                    if (!targetName.isPresent()) {
                        plugin.getLogger().warning(String.format("Failed to retrieve the username for the xuid %s", targetXuid));
                        continue;
                    }
                    FriendRequest request = new FriendRequest(targetXuid, targetName.get());
                    profile.addOutgoingFriendRequest(request);
                }

            } catch (SQLException exception) {
                plugin.getLogger().error(String.format("Failed to load outgoing friend request data for %s while running loadFriendData", player.getName()));
                plugin.getLogger().error(exception.toString());
                return;
            } finally {
                if (fetchOutgoingFriendRequestsQueryStmt != null) {
                    DatabaseUtility.closeQuietly(fetchOutgoingFriendRequestsQueryStmt);
                }
                DatabaseUtility.closeQuietly(wrapper);
            }

            friendRequestsCache.put(xuid, profile);

        }, true);
    }

    /**
     * Unload friend data for a player
     * @param player
     */
    public void unloadFriendData (Player player) {
        String xuid = player.getLoginChainData().getXUID();
        friendsCache.remove(xuid);
        friendRequestsCache.remove(xuid);
    }

    /**
     * Teleport to a friend
     * @param player The player who is being teleported
     * @param friend Their friend
     * @return If the teleport was a success
     */
    public boolean teleportToFriend (Player player, Friend friend) {
        Optional<PlayerServerLocation> location = friend.getLocation();
        if (location.isPresent()) {
            if (player.getServer().getIp().equals(location.get().getIp()) && player.getServer().getPort() == location.get().getPort()) {
                if (gameAPIEnabled()) {
                    Optional<Player> targetPlayer = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(friend.getXuid())).findAny();
                    if (!targetPlayer.isPresent()) {
                        return false;
                    }
                    if (!NewGamesAPI1.getGameManager().getPlayerLookup().containsKey(targetPlayer.get().getUniqueId())) {
                        return false;
                    }
                    return NewGamesAPI1.getGameManager().getPlayerLookup().get(targetPlayer.get().getUniqueId()).addPlayerToGame(player);
                }
                return false;
            }
            TransferPacket packet = new TransferPacket();
            packet.address = location.get().getIp();
            packet.port = location.get().getPort();
            player.dataPacket(packet);
            return true;
        }
        return false;
    }

    public void openFriendsMenu (Player player) {
        FormWindowSimple form = new FormWindowSimple("Friends", "");
        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
            int onlineFriends;
            int incomingFriendRequests;
            try {
                onlineFriends = (int)getFriends(player.getLoginChainData().getXUID()).stream().filter(f -> f.isOnline()).count();
                incomingFriendRequests = getIncomingFriendRequests(player.getLoginChainData().getXUID()).size();
            } catch (SQLException exception) {
                plugin.getLogger().error("Failed to retrieve friends/incoming requests when opening GUI");
                plugin.getLogger().error(exception.toString());
                plugin.getServer().getScheduler().scheduleTask(plugin, () -> player.sendMessage(
                        Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An internal error has occurred.", TextFormat.RED)
                ));
                return;
            }
            form.addButton(new ElementButton(String.format("View Friends %s", onlineFriends > 0 ? String.format("(%d)", onlineFriends) : "")));
            form.addButton(new ElementButton(String.format("View Incoming Friend Requests %s", incomingFriendRequests > 0 ? String.format("(%d)", incomingFriendRequests) : "")));
            form.addButton(new ElementButton("View Outgoing Friend Requests"));
            plugin.getServer().getScheduler().scheduleTask(plugin, () ->
                    formData.put(player.showFormWindow(form), new FriendsFormData(FriendsFormState.MAIN_MENU))
            );
        }, true);
    }

    public void openFriendsListMenu (Player player) {
        FormWindowSimple form = new FormWindowSimple("Friends", "");
        form.addButton(new ElementButton("Add Friend"));
        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
            Collection<Friend> friends;
            try {
                friends = getFriends(player.getLoginChainData().getXUID());
            } catch (SQLException exception) {
                plugin.getLogger().error("Failed to retrieve friends when opening friends list menu");
                plugin.getLogger().error(exception.toString());
                plugin.getServer().getScheduler().scheduleTask(plugin, () -> player.sendMessage(
                        Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An internal error has occurred.", TextFormat.RED)
                ));
                return;
            }
            List<Friend> sortedFriendsList = new ArrayList<>(friends);
            sortedFriendsList.sort(new FriendComparator(plugin));
            for (Friend friend : sortedFriendsList) {
                form.addButton(new ElementButton(String.format("%s - %s", friend.getUsername(), friend.getLocation().isPresent() ? String.format("%sOnline", TextFormat.GREEN) : String.format("%sOffline", TextFormat.RED))));
            }
            plugin.getServer().getScheduler().scheduleTask(plugin, () ->
                    formData.put(player.showFormWindow(form), new FriendsFormData(FriendsFormState.VIEW_FRIENDS_LIST))
            );
        }, true);
    }

    public void openFriendRequestsMenu (Player player, boolean incoming) {
        FormWindowSimple form = new FormWindowSimple(incoming ? "Incoming Friend Requests" : "Outgoing Friend Requests", incoming ? "" : "Which friend request would you like to cancel?");
        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
            Collection<FriendRequest> requests;
            try {
                requests = incoming ? getIncomingFriendRequests(player.getLoginChainData().getXUID()) : getOutgoingFriendRequests(player.getLoginChainData().getXUID());
            } catch (SQLException exception) {
                plugin.getLogger().error("Failed to retrieve friend requests when opening friends requests menu");
                plugin.getLogger().error(exception.toString());
                plugin.getServer().getScheduler().scheduleTask(plugin, () -> player.sendMessage(
                        Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An internal error has occurred.", TextFormat.RED)
                ));
                return;
            }
            List<FriendRequest> sortedRequests = new ArrayList<>(requests);
            sortedRequests.sort(new RequestsComparator(plugin));
            for (FriendRequest request : sortedRequests) {
                form.addButton(new ElementButton(request.getUsername()));
            }
            plugin.getServer().getScheduler().scheduleTask(plugin, () ->
                    formData.put(player.showFormWindow(form), new FriendsFormData(incoming ? FriendsFormState.VIEW_INCOMING_REQUESTS : FriendsFormState.VIEW_OUTGOING_REQUESTS))
            );
        }, true);
    }

    public void openFriendInformation (Player player, Friend friend) {
        boolean isInThisServer = friend.getLocation().isPresent() && friend.getLocation().get().getIp().equals(plugin.getServer().getIp()) && friend.getLocation().get().getPort() == plugin.getServer().getPort();
        FormWindowSimple form = new FormWindowSimple(friend.getUsername(), "");
        if ((friend.isOnline() && !isInThisServer) || (isInThisServer && gameAPIEnabled())) {
            form.addButton(new ElementButton(String.format("%sTeleport To Friend", TextFormat.GREEN)));
        }
        form.addButton(new ElementButton(String.format("%sRemove Friend", TextFormat.RED)));
        formData.put(player.showFormWindow(form), new FriendsFormData(FriendsFormState.FRIEND_INFORMATION, friend.getXuid()));
    }

    public void openIncomingFriendRequest (Player player, FriendRequest request) {
        FormWindowSimple form = new FormWindowSimple(request.getUsername(), "");
        form.addButton(new ElementButton(String.format("%sAccept", TextFormat.GREEN)));
        form.addButton(new ElementButton(String.format("%sDecline", TextFormat.RED)));
        formData.put(player.showFormWindow(form), new FriendsFormData(FriendsFormState.MANAGE_INCOMING_REQUEST, request.getXuid()));
    }

    public void openAddFriend (Player player) {
        FormWindowCustom form = new FormWindowCustom("Add Friend");
        form.addElement(new ElementInput("What is their username?"));
        formData.put(player.showFormWindow(form), new FriendsFormData(FriendsFormState.ADD_FRIEND));
    }

    @EventHandler
    public void onPlayerJoin (PlayerJoinEvent event) {
        loadFriendData(event.getPlayer());
        Friend playerFriend = new Friend(event.getPlayer().getLoginChainData().getXUID(), event.getPlayer().getLoginChainData().getUsername(), Optional.of(new PlayerServerLocation(plugin.getServer().getIp(), plugin.getServer().getPort())));
        synchronized (friendsCache) {
            for (String xuid : friendsCache.keySet()) {
                if (friendsCache.get(xuid).contains(playerFriend)) {
                    Friend oldFriendObj = friendsCache.get(xuid).stream().filter(f -> f.getXuid().equals(playerFriend.getXuid())).findAny().get();
                    if (!oldFriendObj.isOnline()) {
                        oldFriendObj.setLocation(playerFriend.getLocation());
                        Optional<Player> player = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(xuid)).findAny();
                        if (player.isPresent()) {
                            FriendOnlineEvent friendEvent = new FriendOnlineEvent(player.get(), oldFriendObj);
                            plugin.getServer().getPluginManager().callEvent(friendEvent);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit (PlayerQuitEvent event) {
        unloadFriendData(event.getPlayer());
        Friend playerFriend = new Friend(event.getPlayer().getLoginChainData().getXUID(), event.getPlayer().getLoginChainData().getUsername());
        synchronized (friendsCache) {
            for (String xuid : friendsCache.keySet()) {
                if (friendsCache.get(xuid).contains(playerFriend)) {
                    Friend oldFriendObj = friendsCache.get(xuid).stream().filter(f -> f.getXuid().equals(playerFriend.getXuid())).findAny().get();
                    if (oldFriendObj.isOnline()) {
                        oldFriendObj.setLocation(Optional.empty());
                        Optional<Player> player = plugin.getServer().getOnlinePlayers().values().stream().filter(p -> p.getLoginChainData().getXUID().equals(xuid)).findAny();
                        if (player.isPresent()) {
                            FriendOfflineEvent friendEvent = new FriendOfflineEvent(player.get(), oldFriendObj);
                            plugin.getServer().getPluginManager().callEvent(friendEvent);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onFormResponse (PlayerFormRespondedEvent event) {
        if (formData.containsKey(event.getFormID())) {
            FriendsFormData data = formData.get(event.getFormID());
            formData.remove(event.getFormID());
            if (!event.wasClosed()) {

                switch (data.getState()) {
                    case MAIN_MENU: {
                        FormResponseSimple response = (FormResponseSimple)event.getResponse();
                        switch (response.getClickedButtonId()) {
                            case 0:
                                openFriendsListMenu(event.getPlayer());
                                break;
                            case 1:
                                openFriendRequestsMenu(event.getPlayer(), true);
                                break;
                            case 2:
                                openFriendRequestsMenu(event.getPlayer(), false);
                                break;
                        }
                    }
                    break;
                    case VIEW_FRIENDS_LIST: {
                        FormResponseSimple response = (FormResponseSimple)event.getResponse();
                        if (response.getClickedButtonId() == 0) {
                            openAddFriend(event.getPlayer());
                            return;
                        }
                        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                            Collection<Friend> friends;
                            try {
                                friends = getFriends(event.getPlayer().getLoginChainData().getXUID());
                            } catch (SQLException exception) {
                                plugin.getLogger().error("An error has occurred while trying to get retrieve the player's friends via /friend");
                                plugin.getLogger().error(exception.toString());
                                plugin.getServer().getScheduler().scheduleTask(plugin, () -> event.getPlayer().sendMessage(
                                        Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An internal error has occurred.", TextFormat.RED)
                                ));
                                return;
                            }

                            List<Friend> friendList = new ArrayList<>(friends);
                            friendList.sort(new FriendComparator(plugin));

                            int index = response.getClickedButtonId() - 1;
                            if (index >= friendList.size()) {
                                return;
                            }
                            Friend friend = friendList.get(index);
                            plugin.getServer().getScheduler().scheduleTask(plugin, () -> openFriendInformation(event.getPlayer(), friend));
                        }, true);
                    }
                    break;
                    case FRIEND_INFORMATION: {
                        FormResponseSimple response = (FormResponseSimple)event.getResponse();
                        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                            String targetXuid = data.getTarget().get();
                            Optional<Friend> friend;
                            try {
                                friend = getFriends(event.getPlayer().getLoginChainData().getXUID()).stream().filter(f -> f.getXuid().equals(targetXuid)).findAny();
                            } catch (SQLException exception) {
                                plugin.getLogger().error("Failed to fetch friends via friends GUI when viewing friend info");
                                plugin.getLogger().error(exception.toString());
                                plugin.getServer().getScheduler().scheduleTask(plugin, () -> event.getPlayer().sendMessage(
                                        Utility.generateServerMessage("ERROR", TextFormat.DARK_AQUA, "An internal error occurred", TextFormat.RED)
                                ));
                                return;
                            }

                            if (!friend.isPresent()) {
                                return;
                            }

                            if ((friend.get().isOnline() && response.getClickedButtonId() == 1) || !friend.get().isOnline()) {
                                // Remove Friend
                                boolean removed;
                                try {
                                    removed = removeFriend(event.getPlayer().getLoginChainData().getXUID(), friend.get().getXuid());
                                } catch (SQLException exception) {
                                    plugin.getLogger().error("Failed to remove a player as a friend via GUI");
                                    plugin.getLogger().error(exception.toString());
                                    plugin.getServer().getScheduler().scheduleTask(plugin, () -> event.getPlayer().sendMessage(
                                            Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("An error occurred while removing \"%s\" from your friends list.", friend.get().getUsername()), TextFormat.RED)
                                    ));
                                    return;
                                }
                                if (!removed) {
                                    plugin.getServer().getScheduler().scheduleTask(plugin, () -> event.getPlayer().sendMessage(
                                            Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, "Unable to remove friend. Are you sure they're on your friends list?", TextFormat.RED)
                                    ));
                                }
                                return;
                            }
                            // Teleport
                            plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                                if (!teleportToFriend(event.getPlayer(), friend.get())) {
                                    event.getPlayer().sendMessage(
                                            Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, "Unable to teleport you to their server!", TextFormat.RED)
                                    );
                                }
                            });


                        }, true);
                    }
                    break;
                    case ADD_FRIEND: {
                        FormResponseCustom response = (FormResponseCustom)event.getResponse();
                        String name = response.getInputResponse(0);

                        if (name.equalsIgnoreCase(event.getPlayer().getLoginChainData().getUsername())) {
                            event.getPlayer().sendMessage(
                                    Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, "You cannot send a friend request to yourself!", TextFormat.RED)
                            );
                            return;
                        }

                        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                            Optional<String> xuid;
                            try {
                                xuid = PlayerRegistry.getPlayerXuidByName(name);
                            } catch (SQLException exception) {
                                plugin.getLogger().error("Failed to retrieve xuid of target when adding friend via GUI");
                                plugin.getLogger().error(exception.toString());
                                plugin.getServer().getScheduler().scheduleTask(plugin, () -> event.getPlayer().sendMessage(
                                        Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An internal error has occurred.", TextFormat.RED)
                                ));
                                return;
                            }

                            if (!xuid.isPresent()) {
                                plugin.getServer().getScheduler().scheduleTask(plugin, () -> event.getPlayer().sendMessage(
                                        Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, String.format("No player by the name of \"%s\" could be found.", name), TextFormat.RED)
                                ));
                                return;
                            }

                            boolean sentRequest;
                            try {
                                sentRequest = sendFriendRequest(event.getPlayer().getLoginChainData().getXUID(), xuid.get());
                            } catch (SQLException exception) {
                                plugin.getLogger().error("Failed to add friend via GUI");
                                plugin.getLogger().error(exception.toString());
                                plugin.getServer().getScheduler().scheduleTask(plugin, () -> event.getPlayer().sendMessage(
                                        Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An internal error has occurred.", TextFormat.RED)
                                ));
                                return;
                            }

                            if (!sentRequest) {
                                plugin.getServer().getScheduler().scheduleTask(plugin, () -> event.getPlayer().sendMessage(
                                        Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, "You have already sent or have a incoming friend request from this player!", TextFormat.RED)
                                ));
                                return;
                            }

                            plugin.getServer().getScheduler().scheduleTask(plugin, () -> event.getPlayer().sendMessage(
                                    Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, "Friend request sent!", TextFormat.GREEN)
                            ));

                        }, true);
                    }
                    break;
                    case VIEW_INCOMING_REQUESTS:
                    case VIEW_OUTGOING_REQUESTS: {
                        FormResponseSimple response = (FormResponseSimple)event.getResponse();
                        boolean incoming = data.getState() == FriendsFormState.VIEW_INCOMING_REQUESTS;
                        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                            Collection<FriendRequest> requests;
                            try {
                                requests = incoming ? getIncomingFriendRequests(event.getPlayer().getLoginChainData().getXUID()) : getOutgoingFriendRequests(event.getPlayer().getLoginChainData().getXUID());
                            } catch (SQLException exception) {
                                plugin.getLogger().error("Failed to fetch incoming/outgoing requests via GUI");
                                plugin.getLogger().error(exception.toString());
                                plugin.getServer().getScheduler().scheduleTask(plugin, () -> event.getPlayer().sendMessage(
                                        Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An internal error has occurred.", TextFormat.RED)
                                ));
                                return;
                            }
                            List<FriendRequest> sortedRequests = new ArrayList<>(requests);
                            sortedRequests.sort(new RequestsComparator(plugin));

                            int index = response.getClickedButtonId();
                            if (index >= sortedRequests.size()) {
                                return;
                            }
                            FriendRequest request = sortedRequests.get(index);
                            if (incoming) {
                                plugin.getServer().getScheduler().scheduleTask(plugin, () -> openIncomingFriendRequest(event.getPlayer(), request));
                                return;
                            }
                            try {
                                cancelFriendRequest(event.getPlayer().getLoginChainData().getXUID(), request.getXuid());
                            } catch (SQLException exception) {
                                plugin.getLogger().error("Failed to cancel request via GUI");
                                plugin.getLogger().error(exception.toString());
                                plugin.getServer().getScheduler().scheduleTask(plugin, () -> event.getPlayer().sendMessage(
                                        Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An internal error has occurred.", TextFormat.RED)
                                ));
                                return;
                            }
                            plugin.getServer().getScheduler().scheduleTask(plugin, () -> openFriendRequestsMenu(event.getPlayer(), false));
                        }, true);
                    }
                    break;
                    case MANAGE_INCOMING_REQUEST: {
                        FormResponseSimple response = (FormResponseSimple)event.getResponse();
                        plugin.getServer().getScheduler().scheduleTask(plugin, () -> {
                            if (response.getClickedButtonId() == 0) {
                                try {
                                    acceptFriendRequest(event.getPlayer().getLoginChainData().getXUID(), data.getTarget().get());
                                } catch (SQLException exception) {
                                    plugin.getLogger().error("Failed to accept request via GUI");
                                    plugin.getLogger().error(exception.toString());
                                    plugin.getServer().getScheduler().scheduleTask(plugin, () -> event.getPlayer().sendMessage(
                                            Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An internal error has occurred.", TextFormat.RED)
                                    ));
                                }
                                return;
                            }
                            // Deny
                            try {
                                rejectFriendRequest(event.getPlayer().getLoginChainData().getXUID(), data.getTarget().get());
                            } catch (SQLException exception) {
                                plugin.getLogger().error("Failed to reject request via GUI");
                                plugin.getLogger().error(exception.toString());
                                plugin.getServer().getScheduler().scheduleTask(plugin, () -> event.getPlayer().sendMessage(
                                        Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An internal error has occurred.", TextFormat.RED)
                                ));
                            }
                            plugin.getServer().getScheduler().scheduleTask(plugin, () -> event.getPlayer().sendMessage(
                                    Utility.generateServerMessage("FRIENDS", TextFormat.DARK_AQUA, "Rejected friend request", TextFormat.GREEN)
                            ));
                        }, true);
                    }
                    break;


                }

            }
        }
    }

    public void setAsPrimaryManager () {
        instance = this;
    }

    private boolean deleteFriendRequest (String playerXuid, String targetXuid) throws SQLException {
        Optional<FriendRequest> incomingRequest = friendRequestsCache.getOrDefault(targetXuid, new FriendRequestProfile()).getIncomingRequests().stream().filter(request -> request.getXuid().equals(playerXuid)).findAny();  // The target should have this request
        Optional<FriendRequest> outgoingRequest = friendRequestsCache.getOrDefault(playerXuid, new FriendRequestProfile()).getOutgoingRequests().stream().filter(request -> request.getXuid().equals(targetXuid)).findAny();  // The player should have this request.
        ConnectionWrapper wrapper = DatabaseAPI.getConnection("MAIN");
        PreparedStatement deleteFriendRequestQueryStmt = null;
        boolean deleted;
        try {
            deleteFriendRequestQueryStmt = wrapper.prepareStatement(new DatabaseStatement(DELETE_FRIEND_REQUEST_QUERY, new Object[]{ targetXuid, playerXuid }));
            deleted = deleteFriendRequestQueryStmt.executeUpdate() > 0;
        } finally {
            if (deleteFriendRequestQueryStmt != null) {
                DatabaseUtility.closeQuietly(deleteFriendRequestQueryStmt);
            }
            DatabaseUtility.closeQuietly(wrapper);
        }
        if (deleted) {
            synchronized (friendRequestsCache) {
                if (incomingRequest.isPresent() && friendRequestsCache.containsKey(targetXuid) && friendRequestsCache.get(targetXuid).getIncomingRequests().contains(incomingRequest.get())) {
                    friendRequestsCache.get(targetXuid).removeIncomingFriendRequest(incomingRequest.get());
                }
                if (outgoingRequest.isPresent() && friendRequestsCache.containsKey(playerXuid) && friendRequestsCache.get(playerXuid).getOutgoingRequests().contains(outgoingRequest.get())) {
                    friendRequestsCache.get(playerXuid).removeOutgoingFriendRequest(outgoingRequest.get());
                }
            }
        }
        return deleted;
    }

    private boolean gameAPIEnabled () {
        return plugin.getServer().getPluginManager().getPlugin("NewGamesAPI") != null;
    }

    public static FriendsManager get () {
        return instance;
    }

    private static class FriendsFormData {
        private final FriendsFormState state;
        private final Optional<String> target;

        public FriendsFormData (FriendsFormState state) {
            this.state = state;
            target = Optional.empty();
        }

        public FriendsFormData (FriendsFormState state, String target) {
            this.state = state;
            this.target = Optional.of(target);
        }

        public Optional<String> getTarget() {
            return target;
        }

        public FriendsFormState getState () {
            return state;
        }

    }

    private enum FriendsFormState {
        VIEW_FRIENDS_LIST,
        FRIEND_INFORMATION,
        ADD_FRIEND,
        VIEW_INCOMING_REQUESTS,
        VIEW_OUTGOING_REQUESTS,
        MANAGE_INCOMING_REQUEST,
        MAIN_MENU
    }

}
