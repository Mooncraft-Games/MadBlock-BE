package org.madblock.newgamesapi.registry;

import org.madblock.newgamesapi.commands.CommandGame;
import org.madblock.newgamesapi.game.GameID;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

public class GameRegistry {

    private static GameRegistry registryInstance;

    private HashMap<String, GameID> games;

    public GameRegistry() {
        this.games = new HashMap<>();
    }

    /**
     * Registers a Game via it's GameID. Does not accept duplicates. Case-
     * insensitive.
     * @return self for chaining.
     */
    public GameRegistry registerGame(GameID gameID) {
        String id = gameID.getGameIdentifier().toLowerCase();
        if(!games.containsKey(id)){
            games.put(id, gameID);
            CommandGame.refreshParameters();
        }
        return this;
    }

    /**
     * Makes the registry the result provided from GameRegistry#get() and
     * finalizes the instance to an extent.
     */
    public void setAsPrimaryRegistry(){
        if(registryInstance == null) registryInstance = this;
    }

    /** @return the primary instance of the Registry. */
    public static GameRegistry get(){
        return registryInstance;
    }

    public Optional<GameID> getGameID(String gameIdentifier){ return Optional.ofNullable(games.get(gameIdentifier.toLowerCase())); }

    public Set<String> getGames() { return games.keySet(); }

}
