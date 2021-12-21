package org.madblock.skywars;

import cn.nukkit.plugin.PluginBase;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.game.GameID;
import org.madblock.newgamesapi.game.GameProperties;
import org.madblock.newgamesapi.kits.KitGroup;
import org.madblock.newgamesapi.util.NGAPIConstants;
import org.madblock.skywars.behaviours.SkywarsGameBehaviour;
import org.madblock.skywars.kits.DefaultKit;
import org.madblock.skywars.kits.DefenderKit;
import org.madblock.skywars.utils.Constants;

public class SkywarsPlugin extends PluginBase {

    private static SkywarsPlugin instance;

    public SkywarsPlugin () {
        instance = this;
    }

    public static PluginBase getInstance () {
        return instance;
    }


    @Override
    public void onEnable () {

        GameProperties skyWarsProperties = new GameProperties(
                GameHandler.AutomaticWinPolicy.OPPOSING_PLAYERS_DEAD
        )
                .setMinimumPlayers(Constants.MINIMUM_PLAYERS)
                .setGuidelinePlayers(Constants.MINIMUM_PLAYERS)
                .setMaximumPlayers(Constants.MAXIMUM_PLAYERS)
                .setCanWorldBeManipulated(true)
                .setFallDamageEnabled(true)
                .setItemDroppingEnabled(true)
                .setItemPickUpEnabled(true)
                .setHungerEnabled(false)
                .setDefaultCountdownLength(5);

        GameProperties skyWarsTourneyProperties = new GameProperties(
                GameHandler.AutomaticWinPolicy.OPPOSING_PLAYERS_DEAD
        )
                .setMinimumPlayers(1)
                .setGuidelinePlayers(1)
                .setMaximumPlayers(Constants.MAXIMUM_PLAYERS)
                .setCanWorldBeManipulated(true)
                .setFallDamageEnabled(true)
                .setItemDroppingEnabled(true)
                .setItemPickUpEnabled(true)
                .setHungerEnabled(false)
                .setTourneyGamemode(true);

        KitGroup group = new KitGroup("skywars", "Skywars", true, new DefaultKit(), new DefenderKit());

        GameID skyWarsGame = new GameID(Constants.GAME_ID,"sw", "Skywars", "Collect loot and be the last one standing!", "skywars", Constants.GAME_MAP_CATEGORY_TYPES.toArray(new String[0]), 1, skyWarsProperties, SkywarsGameBehaviour.class);
        GameID skyWarsTourneyGame = new GameID("tourney_"+Constants.GAME_ID, NGAPIConstants.EVENT_SERVER_ID, "Skywars Tourney", "Collect loot and be the last one standing! Kills, wins, and top 3 placements give points!", "skywars", (String[])Constants.GAME_MAP_CATEGORY_TYPES.toArray(), 1, skyWarsTourneyProperties, SkywarsGameBehaviour.class);


        NewGamesAPI1.getGameRegistry().registerGame(skyWarsGame).registerGame(skyWarsTourneyGame);
        NewGamesAPI1.getKitRegistry().registerKitGroup(group);


    }

}
