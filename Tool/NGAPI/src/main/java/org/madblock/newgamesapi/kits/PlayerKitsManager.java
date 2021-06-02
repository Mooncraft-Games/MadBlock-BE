package org.madblock.newgamesapi.kits;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.response.FormResponseModal;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindowModal;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.level.Sound;
import cn.nukkit.scheduler.AsyncTask;
import cn.nukkit.utils.TextFormat;
import org.madblock.database.ConnectionWrapper;
import org.madblock.database.DatabaseAPI;
import org.madblock.database.DatabaseStatement;
import org.madblock.database.DatabaseUtility;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.game.GameID;
import org.madblock.newgamesapi.game.GameManager;
import org.madblock.newgamesapi.game.HubManager;
import org.madblock.newgamesapi.registry.KitRegistry;
import org.madblock.newgamesapi.rewards.PlayerRewardsProfile;
import org.madblock.newgamesapi.rewards.RewardChunk;
import org.madblock.newgamesapi.rewards.RewardsManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PlayerKitsManager implements Listener {

    private static PlayerKitsManager managerInstance;

    // There's no way someone would have a group id or kit id longer than 30 characters... right?...
    private static final String CREATE_PREF_TABLE_STMT = "CREATE TABLE IF NOT EXISTS kit_preferences ( xuid VARCHAR(16), group_id VARCHAR(30), kit_id VARCHAR(30) )";
    private static final String CREATE_PURCHASES_TABLE_STMT = "CREATE TABLE IF NOT EXISTS kit_purchases ( xuid VARCHAR(16), group_id VARCHAR(30), kit_id VARCHAR(30) )";

    private static final String SELECT_PLAYER_PREFS_STMT = "SELECT kit_id, group_id FROM kit_preferences WHERE xuid=?";
    private static final String SELECT_PLAYER_PURCHASES_STMT = "SELECT kit_id, group_id FROM kit_purchases WHERE xuid=?";

    private static final String REMOVE_OLD_PLAYER_PREF_STMT = "DELETE FROM kit_preferences WHERE xuid=? AND group_id=?";

    private static final String INSERT_PLAYER_PREF_STMT = "INSERT INTO kit_preferences (xuid, group_id, kit_id) VALUES (?, ?, ?)";
    private static final String INSERT_PLAYER_PURCHASE_STMT = "INSERT INTO kit_purchases (xuid, group_id, kit_id) VALUES (? , ?, ?)";

    // Player: [KitGroup: Preference]
    private final Map<String, HashMap<String, PlayerDatabaseKitEntry>> preferences = new ConcurrentHashMap<>();
    private final Map<String, HashMap<String, Set<PlayerDatabaseKitEntry>>> purchases = new ConcurrentHashMap<>();
    private final Map<Integer, Map<String, String>> kitSelectionWindows = new HashMap<>();

    public PlayerKitsManager () {
        ConnectionWrapper wrapper = null;
        PreparedStatement stmt = null;
        try {
            wrapper = DatabaseAPI.getConnection("MAIN");
            stmt = wrapper.prepareStatement(new DatabaseStatement(CREATE_PREF_TABLE_STMT));
            stmt.execute();
            stmt.close();
            stmt = wrapper.prepareStatement(new DatabaseStatement(CREATE_PURCHASES_TABLE_STMT));
            stmt.execute();
        } catch (SQLException exception) {
            exception.printStackTrace();
            NewGamesAPI1.getPlgLogger().critical("Cannot create preference/purchases table. These systems will not load data!");
        } finally {
            if (stmt != null) {
                DatabaseUtility.closeQuietly(stmt);
            }
            DatabaseUtility.closeQuietly(wrapper);
        }

    }

    /**
     * Makes the manager the result provided from PlayerKitsManager#get() and
     * finalizes the instance to an extent.
     */
    public void setAsPrimaryManager(){
        if(managerInstance == null){
            managerInstance = this;
            NewGamesAPI1.get().getServer().getPluginManager().registerEvents(this, NewGamesAPI1.get());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        if (event.getPlayer().getLoginChainData().isXboxAuthed()) {
            String xuid = event.getPlayer().getLoginChainData().getXUID();
            if(!preferences.containsKey(xuid)){
                loadPreference(xuid);
                loadKitPurchases(xuid);
            }
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event){
        if (event.getPlayer().getLoginChainData().isXboxAuthed()) {
            String xuid = event.getPlayer().getLoginChainData().getXUID();
            if(preferences.containsKey(xuid)){
                unloadPreference(xuid, true);
                unloadKitPurchases(xuid, true);
            }
        }
    }

    /**
     * Whether or not a player owns a kit
     * @param player The player
     * @param group Kit group
     * @param kitID Kit id
     * @return Whether or not the player owns the kit
     */
    public boolean playerOwnsKit(Player player, KitGroup group, String kitID) {

        if (!group.getGroupKits().containsKey(kitID)) {
            return false;
        }
        if (group.getGroupKits().get(kitID).getCost() == 0) {
            return true;
        }
        String xuid = player.getLoginChainData().getXUID();

        synchronized (purchases) {
            if (purchases.containsKey(xuid)) {
                Map<String, Set<PlayerDatabaseKitEntry>> playerPurchases = purchases.get(xuid);
                if (!playerPurchases.containsKey(group.getGroupID())) {
                    return false;
                }
                for (PlayerDatabaseKitEntry entry : purchases.get(xuid).get(group.getGroupID())) {
                    if (entry.getKitId().equals(kitID)) {
                        return true;
                    }
                }
            }
        }
        return false;

    }

    /**
     * Give a player a kit they do not own
     * @param player The player
     * @param group The kit group
     * @param kitID The kit
     * @return Whether or not it was a success
     */
    public boolean givePlayerKit (Player player, KitGroup group, String kitID) {

        if (!group.getGroupKits().containsKey(kitID)) {
            return false;
        }
        String xuid = player.getLoginChainData().getXUID();

        synchronized (purchases) {
            purchases.putIfAbsent(xuid, new HashMap<>());

            Map<String, Set<PlayerDatabaseKitEntry>> playerPurchases = purchases.get(xuid);
            if (!playerPurchases.containsKey(group.getGroupID())) {
                playerPurchases.put(group.getGroupID(), new HashSet<>());
            }
            return playerPurchases.get(group.getGroupID()).add(new PlayerDatabaseKitEntry(kitID, true));
        }

    }

    /**
     * Sets a players preference for a kit within a kit group.
     * @param player the player who's preferences are being edited.
     * @param group the group in which the preference is being changed.
     * @param kitID the kit that should be make the preference.
     * @return true if the preference was updated.
     */
    public boolean setPreference (Player player, KitGroup group, String kitID) {
        if(group.getGroupKits().containsKey(kitID.toLowerCase())){
            synchronized (preferences) {
                preferences.putIfAbsent(player.getLoginChainData().getXUID(), new HashMap<>());
                preferences.get(player.getLoginChainData().getXUID()).put(group.getGroupID(), new PlayerDatabaseKitEntry(kitID.toLowerCase(), true));
                return true;
            }
        }
        return false;
    }

    /**
     * Sets a players preference to the default kit of a kit group.
     * @param player the player who's preferences are being edited.
     * @param group the group in which the preference is being changed to the default.
     * @return true if the preference was updated.
     */
    public boolean setPreferenceToDefault (Player player, KitGroup group) {
        return setPreference(player, group, group.getDefaultKitID());
    }


    /**
     * Fetches the kit ID for a player's kit preference of a specified kitgroup.
     * @param player the player who's kit preferences you want to load.
     * @param kitGroup the group which you want to load the preference kit for.
     * @return the kit ID the player prefers. If there's no preference, it returns the default kit for the group
     */
    public String getPlayerPreferenceForGroup(Player player, KitGroup kitGroup){
        Optional<String> kit = getPlayerPreferenceForGroupWithoutDefault(player, kitGroup);
        return kit.orElse(kitGroup.getDefaultKitID());
    }

    /**
     * Fetches the kit ID for a player's kit preference of a specified kitgroup.
     * @param player the player who's kit preferences you want to load.
     * @param kitGroup the group which you want to load the preference kit for.
     * @return the kit ID the player prefers. If there's no preference, it returns an empty optional.
     */
    public Optional<String> getPlayerPreferenceForGroupWithoutDefault(Player player, KitGroup kitGroup){
        synchronized (preferences) {
            if(preferences.containsKey(player.getLoginChainData().getXUID())){
                HashMap<String, PlayerDatabaseKitEntry> localPrefs = preferences.get(player.getLoginChainData().getXUID());
                String kitGroupID = kitGroup.getGroupID();
                if(localPrefs.containsKey(kitGroupID)){
                    return Optional.of(localPrefs.get(kitGroupID).getKitId());
                }
            }
        }
        return Optional.empty();
    }

    public Optional<HashMap<String, PlayerDatabaseKitEntry>> getPlayerPreferences(Player player){
        return Optional.ofNullable(preferences.get(player.getLoginChainData().getXUID()));
    }

    /**
     * Used to store all unsaved data into the database before the server is stopped.
     * Thread blocking.
     */
    public void clearDatabaseQueue () {
        NewGamesAPI1.get().getLogger().info("Clearing players kits queue...");
        Iterator<String> prefIterator = preferences.keySet().iterator();
        while (prefIterator.hasNext()) {
            unloadPreference(prefIterator.next(), false);
        }
        Iterator<String> purchasesIterator = purchases.keySet().iterator();
        while (purchasesIterator.hasNext()) {
            unloadKitPurchases(purchasesIterator.next(), false);
        }
        NewGamesAPI1.get().getLogger().info("Pushed all player kit updates to database!");
    }

    public void sendKitGroupSelectionWindow (Player player) {
        FormWindowSimple window = new FormWindowSimple("Kit Selector", "Which game would you like to change your kit for?");
        KitRegistry.get().getAllKitGroups().stream()
                .sorted()
                .filter(group -> {
                    Optional<KitGroup> kGroup = KitRegistry.get().getKitGroup(group);
                    return kGroup.isPresent() && kGroup.get().isVisibleInKitGroupSelector();
                })
                .forEach(groupId -> {
                    window.addButton(new ElementButton(KitRegistry.get().getKitGroup(groupId).get().getDisplayName()));
                });
        Map<String, String> windowData = new HashMap<>();
        windowData.put("stage", "kit_group_selection");
        kitSelectionWindows.put(player.showFormWindow(window), windowData);
    }

    public void sendKitSelectionWindow (Player player, KitGroup group) {
        PlayerKitsManager playerKitManager = PlayerKitsManager.get();
        FormWindowSimple window = new FormWindowSimple("Kit Selector", "");
        String selectedKitID = playerKitManager.getPlayerPreferenceForGroup(player, group);
        List<Kit> orderedKits = group.getGroupKits().values().stream().sorted(new KitDisplayComparator(player, group)).collect(Collectors.toList());
        for (Kit kit : orderedKits) {
            if (kit.isVisibleInKitSelector()) {
                String kitButtonText;
                if (playerKitManager.playerOwnsKit(player, group, kit.getKitID())) {
                    if (selectedKitID == kit.getKitID()) {
                        kitButtonText = String.format("%s\n%s%sSELECTED", kit.getKitDisplayName(), TextFormat.BOLD, TextFormat.DARK_GREEN);
                    } else {
                        kitButtonText = kit.getKitDisplayName();
                    }
                } else {
                    kitButtonText = String.format("%s\n%s%s%s COINS", kit.getKitDisplayName(), TextFormat.BOLD, TextFormat.RED, kit.getCost());
                }
                window.addButton(new ElementButton(kitButtonText));
            }
        }

        Map<String, String> windowData = new HashMap<>();
        windowData.put("kit_group_id", group.getGroupID());
        windowData.put("stage", "kit_selection");
        kitSelectionWindows.put(player.showFormWindow(window), windowData);
    }

    public void sendKitPurchasedWindow (Player player, KitGroup group, String kitId) {
        Kit kit = group.getGroupKits().getOrDefault(kitId, group.getDefaultKit());
        FormWindowModal window = new FormWindowModal(
                kit.getKitDisplayName(),
                String.format(kit.getKitDescription()),
                "Select Kit",
                "Go Back"
        );

        Map<String, String> windowData = new HashMap<>();
        windowData.put("kit_group_id", group.getGroupID());
        windowData.put("kit_id", kitId);
        windowData.put("stage", "kit_select");
        kitSelectionWindows.put(player.showFormWindow(window), windowData);
    }

    public void sendKitPurchaseWindow (Player player, KitGroup group, String kitId) {
        Kit kit = group.getGroupKits().getOrDefault(kitId, group.getDefaultKit());
        FormWindowModal window = new FormWindowModal(
                kit.getKitDisplayName(),
                kit.getKitDescription(),
                String.format("Purchase Kit - %s Coins", kit.getCost()),
                "Go Back"
        );


        Map<String, String> windowData = new HashMap<>();
        windowData.put("kit_group_id", group.getGroupID());
        windowData.put("kit_id", kitId);
        windowData.put("stage", "kit_purchase");
        kitSelectionWindows.put(player.showFormWindow(window), windowData);
    }

    @EventHandler
    public void onFormResponse (PlayerFormRespondedEvent event) {
        if (kitSelectionWindows.containsKey(event.getFormID())) {

            Map<String, String> windowData = kitSelectionWindows.get(event.getFormID());
            kitSelectionWindows.remove(event.getFormID());

            if (event.wasClosed()) {
                return;
            }

            String stage = windowData.get("stage");

            if (stage == "kit_group_selection") {
                FormResponseSimple response = (FormResponseSimple)event.getResponse();
                KitGroup selectedGroup = KitRegistry.get().getKitGroup(KitRegistry.get().getAllKitGroups().stream()
                    .sorted()
                    .filter(group -> {
                        Optional<KitGroup> kGroup = KitRegistry.get().getKitGroup(group);
                        return kGroup.isPresent() && kGroup.get().isVisibleInKitGroupSelector();
                    })
                    .collect(Collectors.toList())
                    .get(response.getClickedButtonId())
                ).get();
                sendKitSelectionWindow(event.getPlayer(), selectedGroup);

            } else if (stage == "kit_selection") {

                KitGroup group = KitRegistry.get().getKitGroup(windowData.get("kit_group_id")).get();
                FormResponseSimple response = (FormResponseSimple)event.getResponse();
                // Okay, send them the kit description
                List<Kit> orderedKits = group.getGroupKits().values().stream().sorted(new KitDisplayComparator(event.getPlayer(), group)).collect(Collectors.toList());
                Kit selectedKit = orderedKits.get(response.getClickedButtonId());
                if (PlayerKitsManager.get().playerOwnsKit(event.getPlayer(), group, selectedKit.getKitID())) {
                    sendKitPurchasedWindow(event.getPlayer(), group, selectedKit.getKitID());
                    return;
                }
                sendKitPurchaseWindow(event.getPlayer(), group, selectedKit.getKitID());
            } else if (stage == "kit_select") {
                KitGroup group = KitRegistry.get().getKitGroup(windowData.get("kit_group_id")).get();
                FormResponseModal response = (FormResponseModal)event.getResponse();
                // Did they select the kit?
                if (response.getClickedButtonId() == 0) {
                    Kit kit = group.getGroupKits().getOrDefault(windowData.get("kit_id"), group.getDefaultKit());
                    PlayerKitsManager.get().setPreference(event.getPlayer(), group, kit.getKitID());
                    event.getPlayer().sendMessage(
                            Utility.generateServerMessage("KITS", TextFormat.BLUE, String.format("You equipped the %s%s%s%s %skit!", TextFormat.BOLD, TextFormat.YELLOW, kit.getKitDisplayName(), TextFormat.RESET, TextFormat.GRAY), TextFormat.GRAY)
                    );

                    // Make sure we aren't in the hub/active game.
                    if (GameManager.get().getPlayerLookup().containsKey(event.getPlayer().getUniqueId())) {
                        GameHandler handler = GameManager.get().getPlayerLookup().get(event.getPlayer().getUniqueId());
                        for (GameID gameID : HubManager.get().getHubGames().values()) {
                            if (gameID.getGameServerID() == handler.getGameID().getGameServerID() || handler.getGameState() == GameHandler.GameState.MAIN_LOOP) {
                                return;
                            }
                        }
                        kit.applyKit(event.getPlayer(), handler, true);
                    }
                    return;
                }
                sendKitSelectionWindow(event.getPlayer(), group);
            } else if (stage == "kit_purchase") {
                KitGroup group = KitRegistry.get().getKitGroup(windowData.get("kit_group_id")).get();
                FormResponseModal response = (FormResponseModal)event.getResponse();
                // Did they purchase the kit?
                if (response.getClickedButtonId() == 0) {
                    Kit kit = group.getGroupKits().getOrDefault(windowData.get("kit_id"), group.getDefaultKit());
                    Optional<PlayerRewardsProfile> profile = RewardsManager.get().getRewards(event.getPlayer());
                    if (!profile.isPresent()) {
                        event.getPlayer().sendMessage(
                                Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An error occurred while trying to load your data. Please try again later!", TextFormat.RED)
                        );
                        return;
                    }
                    if (profile.get().getCoins() < kit.getCost()) {
                        event.getPlayer().sendMessage(
                                Utility.generateServerMessage("KITS", TextFormat.BLUE, String.format("You need %s%s more coins%s to be able to purchase this kit!", TextFormat.YELLOW, kit.getCost() - profile.get().getCoins(), TextFormat.RED), TextFormat.RED)
                        );
                        return;
                    }
                    NewGamesAPI1.get().getServer().getScheduler().scheduleTask(NewGamesAPI1.get(), () -> {
                        boolean success;
                        try {
                            success = profile.get().addRewards(new RewardChunk("", "", 0, -kit.getCost()));
                        } catch (SQLException exception) {

                            return;
                        }

                        NewGamesAPI1.get().getServer().getScheduler().scheduleTask(NewGamesAPI1.get(), () -> {

                            if (!success) {
                                NewGamesAPI1.get().getServer().getScheduler().scheduleTask(NewGamesAPI1.get(), () -> event.getPlayer().sendMessage(
                                        Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "An error occurred while trying to purchsae the kit!", TextFormat.RED)
                                ));
                                return;
                            }

                            PlayerKitsManager.get().givePlayerKit(event.getPlayer(), group, kit.getKitID());
                            PlayerKitsManager.get().setPreference(event.getPlayer(), group, kit.getKitID());
                            event.getPlayer().getLevel().addSound(event.getPlayer().getPosition(), Sound.RANDOM_LEVELUP, 1f, 1f, event.getPlayer());
                            event.getPlayer().sendMessage(
                                    Utility.generateServerMessage("KITS", TextFormat.BLUE, String.format("You purchased and equipped the %s%s%s%s %skit!", TextFormat.BOLD, TextFormat.YELLOW, kit.getKitDisplayName(), TextFormat.RESET, TextFormat.GRAY), TextFormat.GRAY)
                            );

                            // Make sure we aren't in the hub/active game.
                            if (GameManager.get().getPlayerLookup().containsKey(event.getPlayer().getUniqueId())) {
                                GameHandler handler = GameManager.get().getPlayerLookup().get(event.getPlayer().getUniqueId());
                                for (GameID gameID : HubManager.get().getHubGames().values()) {
                                    if (gameID.getGameServerID() == handler.getGameID().getGameServerID() || handler.getGameState() == GameHandler.GameState.MAIN_LOOP) {
                                        return;
                                    }
                                }
                                kit.applyKit(event.getPlayer(), handler, true);
                            }
                        });

                    }, true);
                    return;
                }
                sendKitSelectionWindow(event.getPlayer(), group);
            }

        }
    }

    /** @return the primary instance of the Manager. */
    public static PlayerKitsManager get(){
        return managerInstance;
    }

    /**
     * Load kit preferences for a player.
     * @param xuid
     */
    private void loadPreference(String xuid) {
        synchronized (preferences) {
            preferences.putIfAbsent(xuid, new HashMap<>());
            NewGamesAPI1.get().getServer().getScheduler().scheduleTask(NewGamesAPI1.get(), () -> {
                ConnectionWrapper wrapper = null;
                PreparedStatement stmt = null;
                try {
                    wrapper = DatabaseAPI.getConnection("MAIN");
                    stmt = wrapper.prepareStatement(new DatabaseStatement(SELECT_PLAYER_PREFS_STMT, new Object[]{ xuid }));
                    ResultSet results = stmt.executeQuery();
                    while (results.next()) {
                        Optional<KitGroup> group = KitRegistry.get().getKitGroup(results.getString("group_id"));
                        if (group.isPresent()) {
                            preferences.get(xuid).put(group.get().getGroupID(), new PlayerDatabaseKitEntry(results.getString("kit_id"), false));
                        }
                    }
                } catch (SQLException exception) {
                    exception.printStackTrace();
                } finally {
                    if (stmt != null) {
                        DatabaseUtility.closeQuietly(stmt);
                    }
                    if (wrapper != null) {
                        DatabaseUtility.closeQuietly(wrapper);
                    }
                }
            }, true);
        }
    }

    /**
     * Unload kit preferences and save it to the database for a player.
     * @param xuid
     * @param apiCleanUp Whether or not this is run when the api is closing.
     */
    private void unloadPreference(String xuid, boolean apiCleanUp) {
        synchronized (preferences) {
            if (preferences.containsKey(xuid)) {
                if (apiCleanUp) {
                    NewGamesAPI1.get().getServer().getScheduler().scheduleAsyncTask(NewGamesAPI1.get(), new AsyncTask() {
                        @Override
                        public void onRun() {
                            savePreferencesToDatabase(xuid);
                            preferences.remove(xuid);
                        }
                    });
                } else {
                    savePreferencesToDatabase(xuid);
                }
            }
        }
    }

    /**
     * Load kit purchases for a player.
     * @param xuid
     */
    private void loadKitPurchases (String xuid) {
        if (!purchases.containsKey(xuid)) {
            purchases.put(xuid, new HashMap<>());
        }
        NewGamesAPI1.get().getServer().getScheduler().scheduleTask(NewGamesAPI1.get(), () -> {
            ConnectionWrapper wrapper = null;
            PreparedStatement stmt = null;
            try {
                wrapper = DatabaseAPI.getConnection("MAIN");
                stmt = wrapper.prepareStatement(new DatabaseStatement(SELECT_PLAYER_PURCHASES_STMT, new Object[]{ xuid }));
                ResultSet results = stmt.executeQuery();
                while (results.next()) {
                    Optional<KitGroup> group = KitRegistry.get().getKitGroup(results.getString("group_id"));
                    if (group.isPresent()) {
                        synchronized (purchases) {
                            purchases.get(xuid).putIfAbsent(group.get().getGroupID(), new HashSet<>());
                            purchases.get(xuid).get(group.get().getGroupID()).add(new PlayerDatabaseKitEntry(results.getString("kit_id"), false));
                        }
                    }
                }
            } catch (SQLException exception) {
                exception.printStackTrace();
            } finally {
                if (stmt != null) {
                    DatabaseUtility.closeQuietly(stmt);
                }
                if (wrapper != null) {
                    DatabaseUtility.closeQuietly(wrapper);
                }
            }
        }, true);
    }

    /**
     * Unload kit purchases and save it to the database for a player.
     * @param xuid
     * @param apiCleanUp Whether or not this is run when the newgamesapi is closing.
     */
    private void unloadKitPurchases (String xuid, boolean apiCleanUp) {
        synchronized (purchases) {
            if (purchases.containsKey(xuid)) {
                if (apiCleanUp) {
                    NewGamesAPI1.get().getServer().getScheduler().scheduleAsyncTask(NewGamesAPI1.get(), new AsyncTask() {
                        @Override
                        public void onRun() {
                            savePurchasesToDatabase(xuid);
                            purchases.remove(xuid);
                        }
                    });
                } else {
                    savePurchasesToDatabase(xuid);
                }
            }
        }
    }

    /**
     * Internal method called to save preference data to database.
     * @param xuid
     */
    private void savePreferencesToDatabase (String xuid) {
        ConnectionWrapper wrapper = null;
        PreparedStatement stmt = null;
        try {
            wrapper = DatabaseAPI.getConnection("MAIN");
            synchronized (preferences) {
                for (String groupId : preferences.get(xuid).keySet()) {
                    PlayerDatabaseKitEntry entry = preferences.get(xuid).get(groupId);
                    if (!entry.isSaved()) {
                        try {
                            stmt = wrapper.prepareStatement(new DatabaseStatement(REMOVE_OLD_PLAYER_PREF_STMT, new Object[]{ xuid, groupId }));
                            stmt.execute();
                            stmt.close();
                            stmt = wrapper.prepareStatement(new DatabaseStatement(INSERT_PLAYER_PREF_STMT, new Object[]{ xuid, groupId, entry.getKitId() }));
                            stmt.execute();
                            stmt.close();
                        } catch (SQLException exception) {
                            exception.printStackTrace();
                        }
                    }
                }
            }
            stmt = null;
        } catch (SQLException exception) {
            exception.printStackTrace();
        } finally {
            if (stmt != null) {
                DatabaseUtility.closeQuietly(stmt);
            }
            if (wrapper != null) {
                DatabaseUtility.closeQuietly(wrapper);
            }
        }
    }

    /**
     * Internal method called to save purchases data to database.
     * @param xuid
     */
    private void savePurchasesToDatabase (String xuid) {
        ConnectionWrapper wrapper = null;
        PreparedStatement stmt = null;
        try {
            wrapper = DatabaseAPI.getConnection("MAIN");
            synchronized (purchases) {
                for (String groupId : purchases.get(xuid).keySet()) {
                    Set<PlayerDatabaseKitEntry> entries = purchases.get(xuid).get(groupId);
                    for (PlayerDatabaseKitEntry entry : entries) {
                        if (!entry.isSaved()) {
                            try {
                                stmt = wrapper.prepareStatement(new DatabaseStatement(INSERT_PLAYER_PURCHASE_STMT, new Object[]{ xuid, groupId, entry.getKitId() }));
                                stmt.execute();
                                stmt.close();
                            } catch (SQLException exception) {
                                exception.printStackTrace();
                            }
                        }
                    }
                }
            }
            stmt = null;
        } catch (SQLException exception) {
            exception.printStackTrace();
        } finally {
            if (stmt != null) {
                DatabaseUtility.closeQuietly(stmt);
            }
            if (wrapper != null) {
                DatabaseUtility.closeQuietly(wrapper);
            }
        }
    }

    private class KitDisplayComparator implements Comparator<Kit> {

        private final Player player;
        private final KitGroup group;
        private final String selectedKitID;

        public KitDisplayComparator (Player player, KitGroup group) {
            this.player = player;
            this.group = group;
            selectedKitID = PlayerKitsManager.get().getPlayerPreferenceForGroup(player, group);
        }

        @Override
        public int compare(Kit kitA, Kit kitB) {
            PlayerKitsManager playerKitsManager = PlayerKitsManager.get();
            if (kitA.getKitID() == selectedKitID) {
                return -1;
            } else if (kitB.getKitID() == selectedKitID) {
                return 1;
            }
            if (!playerKitsManager.playerOwnsKit(player, group, kitA.getKitID()) && playerKitsManager.playerOwnsKit(player, group, kitB.getKitID())) {
                return 1;
            }
            if (!playerKitsManager.playerOwnsKit(player, group, kitB.getKitID()) && playerKitsManager.playerOwnsKit(player, group, kitA.getKitID())) {
                return -1;
            }
            if (playerKitsManager.playerOwnsKit(player, group, kitA.getKitID())) {
                if (playerKitsManager.playerOwnsKit(player, group, kitB.getKitID())) {
                    return 0; // Do not move either kit.
                }
                return -1;
            }
            if (playerKitsManager.playerOwnsKit(player, group, kitB.getKitID())) {
                return 1;
            }
            return 0; // Both kits are not unlocked.
        }
    }

    private static class PlayerDatabaseKitEntry {

        private final String kitId;
        private final boolean saved;

        public PlayerDatabaseKitEntry(String kitId, boolean needsToBeSaved) {
            this.kitId = kitId;
            saved = !needsToBeSaved;
        }

        public String getKitId() {
            return kitId;
        }

        public boolean isSaved () {
            return saved;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof PlayerDatabaseKitEntry) {
                return ((PlayerDatabaseKitEntry)obj).getKitId().equals(getKitId());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return kitId.hashCode();
        }
    }


}
