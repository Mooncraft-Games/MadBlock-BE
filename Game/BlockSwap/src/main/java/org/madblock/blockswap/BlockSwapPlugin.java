package org.madblock.blockswap;

import cn.nukkit.plugin.PluginBase;

import org.madblock.blockswap.kits.DefaultKit;
import org.madblock.blockswap.kits.LeaperKit;
import org.madblock.blockswap.kits.RunnerKit;
import org.madblock.blockswap.utils.BlockSwapConstants;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.game.GameHandler.AutomaticWinPolicy;
import org.madblock.newgamesapi.game.GameID;

import org.madblock.blockswap.behaviours.BlockSwapGameBehaviour;
import org.madblock.newgamesapi.game.GameProperties;
import org.madblock.newgamesapi.kits.KitGroup;
import org.madblock.newgamesapi.util.NGAPIConstants;

public class BlockSwapPlugin extends PluginBase {

    private static PluginBase instance;

    public static PluginBase getInstance () {
        return instance;
    }

    public void onEnable () {

        instance = this;

        String[] gameMapCategoryTypes = {
            "blockswap"
        };

        KitGroup blockSwapKits = new KitGroup("blockswap", "BlockSwap", true, new DefaultKit(), new LeaperKit(), new RunnerKit());
        NewGamesAPI1.getKitRegistry().registerKitGroup(blockSwapKits);

        GameProperties gameProperties = new GameProperties(
                AutomaticWinPolicy.OPPOSING_PLAYERS_DEAD
        )
                .setGuidelinePlayers(BlockSwapConstants.MINIMUM_PLAYERS)
                .setMinimumPlayers(BlockSwapConstants.MINIMUM_PLAYERS)
                .setMaximumPlayers(BlockSwapConstants.MAXIMUM_PLAYERS)
                .setCanWorldBeManipulated(false);

        GameProperties gameTourneyProperties = new GameProperties(
                AutomaticWinPolicy.OPPOSING_PLAYERS_DEAD
        )
                .setGuidelinePlayers(BlockSwapConstants.MINIMUM_PLAYERS)
                .setMinimumPlayers(BlockSwapConstants.MINIMUM_PLAYERS)
                .setMaximumPlayers(BlockSwapConstants.MAXIMUM_PLAYERS)
                .setCanWorldBeManipulated(false)
                .setTourneyGamemode(true);

        GameID game = new GameID(
                "blockswap",
                "bswap",
                "Block Swap",
                "Quick! Stand on the block displayed or you lose!",
                blockSwapKits.getGroupID(),
                gameMapCategoryTypes,
                1,
                gameProperties,
                BlockSwapGameBehaviour.class
        );

        GameID tourney = new GameID(
                "tourney_blockswap",
                NGAPIConstants.EVENT_SERVER_ID,
                "Block Swap Tourney",
                "Quick! Stand on the block displayed or you lose! Points are awarded to the top 3 with a bonus for #1!",
                blockSwapKits.getGroupID(),
                gameMapCategoryTypes,
                1,
                gameTourneyProperties,
                BlockSwapGameBehaviour.class
        );

        NewGamesAPI1.getGameRegistry().registerGame(game).registerGame(tourney);
    }


}