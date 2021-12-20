package org.madblock.newgamesapi.game;

import cn.nukkit.utils.TextFormat;
import org.madblock.lib.stattrack.statistic.ITrackedEntityID;
import org.madblock.newgamesapi.util.Utility;
import org.madblock.newgamesapi.kits.KitGroup;
import org.madblock.newgamesapi.registry.KitRegistry;

public final class GameID implements ITrackedEntityID {

    private transient String[] gameInfoMessageParagraphs;
    private transient String gameInfoMessage;

    private String gameIdentifier;
    private String gameServerID;
    private String gameDisplayName;
    private String gameDescription;
    private String kitGroupID;
    private String[] mapTemplateCategories;
    private int gameVersion;

    private GameProperties gameProperties;

    private Class<? extends GameBehavior> gameBehaviorClass;

    public GameID(String gameIdentifier, String gameServerID, String gameDisplayName, String gameDescription, String kitGroupID, String[] mapTemplateCategories, int gameVersion, GameProperties gameProperties, Class<? extends GameBehavior> gameBehaviorClass) {
        this.gameIdentifier = gameIdentifier.toLowerCase();
        this.gameServerID = gameServerID.toLowerCase();
        this.gameDisplayName = gameDisplayName;
        this.gameDescription = gameDescription;
        this.kitGroupID = kitGroupID.toLowerCase();
        this.mapTemplateCategories = new String[mapTemplateCategories.length];
        for(int i = 0; i < mapTemplateCategories.length; i++){
            this.mapTemplateCategories[i] = mapTemplateCategories[i].toLowerCase();
        }
        this.gameVersion = gameVersion;
        this.gameProperties = gameProperties;
        this.gameBehaviorClass = gameBehaviorClass;

        gameInfoMessageParagraphs = new String[]{
                ""+ TextFormat.BLUE+TextFormat.BOLD+getGameDisplayName()+"\n",
                "",
                getGameDescription(),
                "Version: "+getGameVersion()
        };
        gameInfoMessage = Utility.generateParagraph(gameInfoMessageParagraphs, TextFormat.DARK_AQUA, TextFormat.DARK_AQUA, 35);
    }

    /** @return a fully formatted info box intended to be sent directly to players. */
    public String getGameInfoMessage() { return gameInfoMessage; }
    /** @return a fully formatted set of paragraphs for an info box. */
    public String[] getGameInfoMessageParagraphs() { return gameInfoMessageParagraphs; }

    /** @return the internal id. Will be used to select the games. */
    public String getGameIdentifier() { return gameIdentifier; }
    /** @return the shorthand id. Will be used to mark lobbies and folders. */
    public String getGameServerID() { return gameServerID; }
    /** @return the name of the game. Intended to be displayed in-game. */
    public String getGameDisplayName() { return gameDisplayName; }
    /** @return the description of the game. Intended to be displayed in-game. */
    public String getGameDescription() { return gameDescription; }
    /** @return the kit group ID used by the game. Used to load kit preferences and the kit selection. */
    public String getKitGroupID() { return kitGroupID; }
    /** @return the categories the game supports maps from. */
    public String[] getMapTemplateCategories() { return mapTemplateCategories; }
    /** @return miscellaneous properties related to the operation of the game. */
    public GameProperties getGameProperties() { return gameProperties; }
    /** @return the version of the game. Not much use other than displaying changes. */
    public int getGameVersion() { return gameVersion; }

    /**
     * Provides the kitgroup the the game's available kits.
     */
    public KitGroup getGameKits() { return KitRegistry.get().getKitGroup(kitGroupID).orElse(KitRegistry.DEFAULT); }

    public Class<? extends GameBehavior> getGameBehaviorClass() { return gameBehaviorClass; }

    @Override
    public String getEntityType() {
        return "game";
    }

    @Override
    public String getStoredID() {
        return gameIdentifier;
    }
}
