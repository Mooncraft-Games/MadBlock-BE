package org.madblock.newgamesapi.game;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.exception.GameNotFoundException;
import org.madblock.newgamesapi.exception.InvalidMapIDException;
import org.madblock.newgamesapi.exception.LackOfContentException;
import org.madblock.newgamesapi.exception.PotentialMaxGamesReachedException;
import org.madblock.newgamesapi.map.MapID;
import org.madblock.newgamesapi.map.MapManager;
import org.madblock.newgamesapi.registry.GameRegistry;

import java.util.*;

public class GameManager {

    private static final char[] ALPHABET = new char[]{'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z'};
    private static final int SERVER_RANDOM_LENGTH = 4;
    private static final int MAX_MAP_ATTEMPS = 3;

    private static GameManager managerInstance;

    private HashMap<String, GameHandler> sessions;
    private HashMap<UUID, GameHandler> playerLookup;

    // GameID : ServerID[]
    private HashMap<String, ArrayList<String>> publicGamesList;

    public GameManager(){
        this.sessions = new HashMap<>();
        this.playerLookup = new HashMap<>();

        this.publicGamesList = new HashMap<>();
    }

    public void setAsPrimaryManager(){
        if(managerInstance == null) managerInstance = this;
    }

    public String createGameSession(String gameIdentifier, int cleanupTime) throws PotentialMaxGamesReachedException, GameNotFoundException { return createGameSession(gameIdentifier, cleanupTime, null); }

    public String createGameSession(String gameIdentifier, int cleanupTime, String name) throws PotentialMaxGamesReachedException, GameNotFoundException {
        GameRegistry registry = GameRegistry.get();
        Optional<GameID> retrivedId = registry.getGameID(gameIdentifier);
        if(!retrivedId.isPresent()) throw new GameNotFoundException("Unable to find a registered game with the id: "+gameIdentifier);
        GameID id = retrivedId.get();

        GameBehavior behavior;
        try{
            behavior = id.getGameBehaviorClass().newInstance();
        } catch (Exception err){
            throw new GameNotFoundException("Misconfigured GameBehavior threw exception for game: "+gameIdentifier);
        }

        Optional<String> potentialSessionID = createSessionID(id, 20);
        if(!potentialSessionID.isPresent()){
            throw new PotentialMaxGamesReachedException("The session ID could not be generated due to reaching it's max attempts.");
        }
        String sessionID = potentialSessionID.get();

        ArrayList<MapID> mapPool = new ArrayList<>();
        String[] targetCategories = id.getMapTemplateCategories();

        for(String category: targetCategories){
            Optional<ArrayList<MapID>> potentialMaps = MapManager.get().getMapsFromCategory(category);
            potentialMaps.ifPresent(mapPool::addAll);
        }

        if(mapPool.size() == 0) throw new LackOfContentException("No maps could be loaded for the gamemode: "+gameIdentifier);

        int randomMapIndex = 0;
        GameHandler handler = null;
        for(int i = 0; i < mapPool.size() && i < MAX_MAP_ATTEMPS; i++) {
            try {
                randomMapIndex = new Random().nextInt(mapPool.size());
                MapID selectedMapID = mapPool.get(randomMapIndex);
                Level mapLevel;
                try {
                    Optional<Level> level = MapManager.get().loadTemplateMapFromMapID(sessionID, selectedMapID);
                    if(level.isPresent()){ mapLevel = level.get(); } else { throw new LackOfContentException(""); }
                } catch (Exception err){
                    throw new LackOfContentException("Unable to load map for gamemode: "+gameIdentifier);
                }

                GameHandler gameHandler = new GameHandler(id, behavior, sessionID, selectedMapID, mapLevel, this, cleanupTime);
                handler = gameHandler;
                break;
            } catch (InvalidMapIDException err){
                NewGamesAPI1.getPlgLogger().warning(String.format("Map ID (%s) threw an error when loaded into an active game. Retrying.", mapPool.get(randomMapIndex).getId()));
                mapPool.remove(randomMapIndex);
                err.printStackTrace();
            }
        }
        if(handler == null){ throw new LackOfContentException("There were no valid maps avilable."); }
        if(!publicGamesList.containsKey(gameIdentifier)){
            publicGamesList.put(gameIdentifier, new ArrayList<>());
        }
        sessions.put(sessionID, handler);
        publicGamesList.get(gameIdentifier).add(sessionID);
        return sessionID;
    }

    private Optional<String> createSessionID(GameID id, int maxAttempts){
        for(int i = 0; i < maxAttempts; i++) {
            String gameServerPrefix = id.getGameServerID().toLowerCase();
            String suffix = generateRandomLetters(SERVER_RANDOM_LENGTH);
            String constructedID = gameServerPrefix+"-"+suffix;
            if(!sessions.containsKey(constructedID)){
                return Optional.of(constructedID);
            }
        }
        return Optional.empty();
    }

    /**
     * Caller should first call the session's cleanup functions to ensure
     * all worlds are properly destroyed!
     * @return true if the session was properly removed (False could be an error or it being missing).
     */
    public boolean removeSession(String sessionID) {
        if(sessions.containsKey(sessionID)){
            GameHandler handler = sessions.get(sessionID);
            sessions.remove(sessionID);
            if(publicGamesList.containsKey(handler.getGameID().getGameIdentifier())){
                return publicGamesList.get(handler.getGameID().getGameIdentifier()).remove(sessionID);
            }
            return false;
        }
        return false;
    }

    protected boolean appendPlayerToLookup(Player player, GameHandler handler){
        if(sessions.containsValue(handler) && !((handler.getGameState() == GameHandler.GameState.END) || (handler.getGameState() == GameHandler.GameState.PRE_PREPARE))){
            UUID uuid = player.getLoginChainData().getClientUUID();
            if(playerLookup.containsKey(uuid)) playerLookup.get(uuid).removePlayerFromGame(player, true);
            playerLookup.put(uuid, handler);
            return true;
        }
        return false;
    }

    protected boolean removePlayerFromLookup(Player player, GameHandler handler){
        if(sessions.containsValue(handler)){
            UUID uuid = player.getLoginChainData().getClientUUID();
            if(playerLookup.containsKey(uuid)){
                playerLookup.remove(uuid);
                return true;
            }
        }
        return false;
    }

    public Optional<GameHandler> getSession(String sessionID) {
        return Optional.ofNullable(sessions.get(sessionID));
    }

    public String[] getAllSessionIDs() {
        return sessions.keySet().toArray(new String[0]);
    }

    public Optional<String[]> getSessionIDsForGame(String gameIdentifier) {
        String l = gameIdentifier.toLowerCase();
        if(publicGamesList.containsKey(l)){
            return Optional.of(publicGamesList.get(l).toArray(new String[0]));
        } else {
            return Optional.empty();
        }
    }

    public HashMap<UUID, GameHandler> getPlayerLookup() {
        return new HashMap<>(playerLookup);
    }

    /** @return the primary instance of the Manager. */
    public static GameManager get() { return managerInstance; }

    private static String generateRandomLetters(int length) {
        String fstr = "";
        for(int i = 0; i < length; i++){
            Random r = new Random();
            fstr = fstr.concat(String.valueOf(ALPHABET[r.nextInt(ALPHABET.length)]));
        }
        return fstr;
    }

}
