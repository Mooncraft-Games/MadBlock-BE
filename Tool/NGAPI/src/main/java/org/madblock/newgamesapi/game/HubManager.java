package org.madblock.newgamesapi.game;

import cn.nukkit.Player;
import com.google.gson.*;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.exception.GameNotFoundException;
import org.madblock.newgamesapi.exception.PotentialMaxGamesReachedException;
import org.madblock.newgamesapi.game.internal.hub.GameBehaviorLobby;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;

public class HubManager {

    public static final String HUB_GAME_ID = "mainhub";
    public static final String HUB_KIT_ID = "hub_loadout";
    public static final int HUB_MAX_PLAYERS = 40;
    public static final int TOURNEY_MAX_PLAYERS = 200; //There's no way tourneys will be that big.

    public static final String HUB_NAME = "Hub";
    public static final String HUB_DESCRIPTION = "Welcome.";

    public static final String GAME_HUB_LOCATION_PATH = "./plugins/NGAPI/";
    public static final String GAME_HUB_CONFIGURATION_NAME = "game_hubs.json";

    private static HubManager managerInstance;
    protected static GameProperties defaultHubProperties;

    static {
        defaultHubProperties = new GameProperties(GameHandler.AutomaticWinPolicy.MANUAL_CALLS_ONLY)
                .setMinimumPlayers(0)
                .setGuidelinePlayers(1)
                .setMaximumPlayers(HUB_MAX_PLAYERS)
                .setDefaultCountdownLength(-1)
                .setFallDamageEnabled(false)
                .setFallDamageEnabledPreGame(false)
                .setItemDroppingEnabled(false)
                .setItemDroppingEnabledPreGame(false)
                .setCanPlayersMoveDuringCountdown(true)
                .setCanWorldBeManipulated(true)
                .setHungerEnabled(false)
                .setNatualRegenerationEnabled(true);
    }

    protected GameManager parentManager;
    protected HashMap<String, HashMap<String, GameHandler>> hubWorlds;
    protected HashMap<String, GameID> hubGames;

    protected HashMap<String, String> lastPlayerHubs;

    public HubManager (GameManager parentManager){
        this.parentManager = parentManager;
        this.hubWorlds = new HashMap<>();
        this.hubGames = new HashMap<>();
        this.lastPlayerHubs = new HashMap<>();
    }

    public void setAsPrimaryManager(){
        if(managerInstance == null) managerInstance = this;
    }

    public HubManager registerHubGame(String hubGameID, String displayname, String[] supportedMaps) { return registerHubGame(hubGameID, displayname, null, supportedMaps, null, null); }
    public HubManager registerHubGame(String hubGameID, String displayname, String shorthandID, String[] supportedMaps) { return registerHubGame(hubGameID, displayname, shorthandID, supportedMaps, null, null); }
    public HubManager registerHubGame(String hubGameID, String displayname, String shorthandID, String[] supportedMaps, String icon) { return registerHubGame(hubGameID, displayname, shorthandID, supportedMaps, icon, null); }
    public HubManager registerHubGame(String hubGameID, String displayname, String[] supportedMaps, String icon, GameProperties propertiesOverride) { return registerHubGame(hubGameID, displayname, null, supportedMaps, icon, propertiesOverride); }
    public HubManager registerHubGame(String hubGameID, String displayname, String shorthandID, String[] supportedMaps, String icon, GameProperties propertiesOverride) {
        String shortId = shorthandID == null ? hubGameID : shorthandID;
        GameProperties prop = (propertiesOverride == null ? defaultHubProperties : propertiesOverride).copy();
        prop.setIconPath(icon);

        this.registerHubGame(new GameID(hubGameID, shortId, displayname, HUB_DESCRIPTION, HUB_KIT_ID, supportedMaps, 1, prop, GameBehaviorLobby.class));
        return this;
    }

    public HubManager registerHubGame(GameID id){
        NewGamesAPI1.getGameRegistry().registerGame(id);
        hubGames.put(id.getGameIdentifier().toLowerCase(), id);
        return this;
    }

    public Optional<GameHandler> startHub(String hubTypeID) {
        try {
            String type = hubTypeID == null ? HUB_GAME_ID : hubTypeID;
            String id = GameManager.get().createGameSession(type, 0);
            Optional<GameHandler> handler = GameManager.get().getSession(id);
            if(handler.isPresent()){
                GameHandler h = handler.get();
                if(h.getGameID().getGameProperties().getMinimumPlayers() == 0) {
                    if(!hubWorlds.containsKey(type)){
                        hubWorlds.put(type, new HashMap<>());
                    }
                    HashMap<String, GameHandler> hubs = hubWorlds.get(type);
                    hubs.put(id, h);
                    h.prepare(new Player[0]);
                } else {
                    GameManager.get().removeSession(h.getServerID());
                }
                return handler;
            }
        } catch (PotentialMaxGamesReachedException err){
            NewGamesAPI1.getPlgLogger().warning("Unable to start a new hub world due to a potential max game id combination limit being reached. This could be bad.");
        } catch (GameNotFoundException err){
            NewGamesAPI1.getPlgLogger().emergency("Unable to start a new hub world due to the ID being invalid. This is a bad NGAPI build.");
        } catch (Exception err){
            NewGamesAPI1.getPlgLogger().warning("Unknown Exception thrown when starting a hub world. Hmm");
            err.printStackTrace();
        }
        NewGamesAPI1.getPlgLogger().warning("Unknown Issue occurred when starting a hub world. Hmm");
        return Optional.empty();
    }

    public Optional<GameHandler> getAvailableHub(String type){
        return getAvailableHub(type, null);
    }

    public Optional<GameHandler> getAvailableHub(String type, Player player){
        GameHandler emptyHub = null;

        if(hubWorlds.containsKey(type)) {
            HashMap<String, GameHandler> hubs = hubWorlds.get(type);

            for (GameHandler hub : hubs.values()) {

                if ((hub.getGameState() != GameHandler.GameState.END) && (hub.getPlayers().size() < hub.getGameID().getGameProperties().getMaximumPlayers())) {

                    if((player == null) || (!hub.getPlayers().contains(player))) { // If player field is null OR the hub doesn't contain the player.
                        emptyHub = hub;
                        break;
                    }
                }
            }
        }
        return emptyHub == null ? startHub(type) : Optional.of(emptyHub);
    }

    public void updateLastHubPreference(Player player, String type){
        lastPlayerHubs.put(player.getLoginChainData().getXUID(), type);
    }

    public Optional<String> getLastPlayerHub(Player player) {
        return Optional.ofNullable(lastPlayerHubs.get(player.getLoginChainData().getXUID()));
    }

    public void loadHubTypesConfiguration(){
        NewGamesAPI1.getPlgLogger().info("== Starting GameHub loading. ==");
        File cfgFile = new File(NewGamesAPI1.get().getServer().getDataPath()+GAME_HUB_LOCATION_PATH+GAME_HUB_CONFIGURATION_NAME);

        if(cfgFile.exists() && cfgFile.isFile()){

            NewGamesAPI1.getPlgLogger().info(": > FOUND - Configuration exists.");
            try {
                FileReader r = new FileReader(cfgFile);
                BufferedReader reader = new BufferedReader(r);
                String str = "";

                Iterator<String> i = reader.lines().iterator();
                while (i.hasNext()){
                    str = str.concat(i.next());
                }
                NewGamesAPI1.getPlgLogger().info(": > READ - Configuration was loaded successfully.");

                loadHubTypeConfigFromJson(str);

                NewGamesAPI1.getPlgLogger().info("== Finished GameHub loading. ==");
                return;
            } catch (Exception err){
                err.printStackTrace();
                NewGamesAPI1.getPlgLogger().info(": > FAIL - Check above for stacktrace.");
            }

        } else {

            NewGamesAPI1.getPlgLogger().info(": > MISSING - Attempting to create config.");
            try {
                File location = new File(NewGamesAPI1.get().getServer().getDataPath()+GAME_HUB_LOCATION_PATH);
                location.mkdirs();
                boolean result = cfgFile.createNewFile();
                if (result) {
                    FileWriter w = new FileWriter(cfgFile);
                    BufferedWriter writer = new BufferedWriter(w);
                    writer.write("{ }");
                    writer.close();
                    NewGamesAPI1.getPlgLogger().info(": > PASS - Created config at: " + cfgFile.getAbsolutePath());
                    return;
                } else {
                    NewGamesAPI1.getPlgLogger().info(": > FAIL - Unable to create file for an unknown reason.");
                    return;
                }
            } catch (Exception err){
                err.printStackTrace();
                NewGamesAPI1.getPlgLogger().info(": > FAIL - Check above for stacktrace.");
                return;
            }
        }
        NewGamesAPI1.getPlgLogger().info("== Finished GameHub loading. ==");
    }

    protected boolean loadHubTypeConfigFromJson(String json){
        JsonParser parser = new JsonParser();
        JsonElement rootElement = parser.parse(json);

        if(rootElement instanceof JsonObject){
            JsonObject rootObject = (JsonObject) rootElement;
            JsonElement hubsElement = rootObject.get("hubs");

            if(hubsElement instanceof JsonArray){
                JsonArray hubsArray = (JsonArray) hubsElement;

                NewGamesAPI1.getPlgLogger().info(": > LOAD - Found data, loading.");
                for(JsonElement e: hubsArray){
                    try {

                        if (e instanceof JsonObject) {
                            JsonObject hubObject = (JsonObject) e;
                            JsonElement gameID = hubObject.get("id"); //Primitive, Required
                            JsonElement name = hubObject.get("name"); //Primitive, Optional
                            JsonElement shortName = hubObject.get("short"); //Primitive, Optional
                            JsonElement icon = hubObject.get("icon"); //Primitive, Optional
                            JsonElement maps = hubObject.get("categories"); //Array, Required


                            if (!(gameID instanceof JsonPrimitive)) {
                                NewGamesAPI1.getPlgLogger().info(": > SKIP - Hub missing an 'id' field of type String.");
                                continue;
                            }

                            if (!(maps instanceof JsonArray)) {
                                NewGamesAPI1.getPlgLogger().info(": > SKIP - Hub missing an 'categories' field of type Array.");
                                continue;
                            }

                            JsonPrimitive primitiveGameID = (JsonPrimitive) gameID;
                            JsonArray arrayMaps = (JsonArray) maps;

                            String propertyGameID = primitiveGameID.getAsString();
                            String propertyName = null;
                            String propertyShortName = null;
                            String propertyIcon = null;
                            ArrayList<String> propertyMaps = new ArrayList<>();

                            if(name instanceof JsonPrimitive) {
                                JsonPrimitive primitiveName = (JsonPrimitive) name;
                                propertyName = primitiveName.getAsString();
                            } else {
                                propertyName = HUB_NAME;
                            }

                            if(shortName instanceof JsonPrimitive) {
                                JsonPrimitive primitiveShort = (JsonPrimitive) shortName;
                                propertyShortName = primitiveShort.getAsString();
                            } else {
                                propertyShortName = propertyGameID; //Could be hub too but that may be unintentional.
                            }

                            if(icon instanceof JsonPrimitive) {
                                propertyIcon = icon.getAsString();
                            }

                            for(JsonElement catElement: arrayMaps){

                                if(catElement instanceof JsonPrimitive){
                                    JsonPrimitive categoryPrimitive = (JsonPrimitive) catElement;
                                    String category = categoryPrimitive.getAsString();
                                    if(category != null && category.length() > 0){
                                        propertyMaps.add(category.toLowerCase());
                                    }
                                }
                            }

                            if(propertyMaps.size() == 0){
                                NewGamesAPI1.getPlgLogger().info(": > SKIP - There were no valid supported map categories.");
                                continue;
                            }

                            if(propertyGameID != null && propertyName != null && propertyShortName != null){
                                HubManager.get().registerHubGame(propertyGameID, propertyName, propertyShortName, propertyMaps.toArray(new String[0]), propertyIcon);
                                NewGamesAPI1.getPlgLogger().info(": > PASS - Loaded hubtype '"+propertyGameID+"'.");
                            } else {
                                NewGamesAPI1.getPlgLogger().info(": > SKIP - Something went wrong as a field was null.");
                            }

                        } else {
                            NewGamesAPI1.getPlgLogger().info(": > SKIP - Non-Object property in hubs array.");
                        }

                    } catch (Exception err){
                        NewGamesAPI1.getPlgLogger().info(": > SKIP - Unexpected Error parsing hub type.");
                    }
                }

            } else {
                NewGamesAPI1.getPlgLogger().info(": > FAIL - Missing Array Type 'hubs' property.");
            }

        } else {
            NewGamesAPI1.getPlgLogger().info(": > FAIL - Root element is not an object.");
        }
        return false;
    }

    public GameManager getParentManager() { return parentManager; }
    public HashMap<String, HashMap<String, GameHandler>> getHubWorlds() { return new HashMap<>(hubWorlds); }
    public HashMap<String, GameID> getHubGames() { return hubGames; }

    /** @return the primary instance of the Manager. */
    public static HubManager get() { return managerInstance; }

}
