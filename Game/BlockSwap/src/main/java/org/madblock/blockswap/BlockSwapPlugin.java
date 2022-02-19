package org.madblock.blockswap;

import cn.nukkit.plugin.PluginBase;

import cn.nukkit.utils.Logger;
import org.madblock.blockswap.behaviours.PracticeBlockSwapGameBehaviour;
import org.madblock.blockswap.generator.BSwapGeneratorManager;
import org.madblock.blockswap.generator.builtin.BSGRandom;
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

public class BlockSwapPlugin extends PluginBase {

    private static BlockSwapPlugin instance;

    private BSwapGeneratorManager generatorManager;

    @Override
    public void onEnable() {
        instance = this;

        // -- Managers --

        this.generatorManager = new BSwapGeneratorManager();
        this.generatorManager.setAsPrimaryManager();

        // -- Components --

        //BSwapGeneratorManager.get().registerGenerator(new BSG2DNoise());
        //BSwapGeneratorManager.get().registerGenerator(new BSGMaskedMultigen()); - Needs more generation options
        //BSwapGeneratorManager.get().registerGenerator(new BSGNoiseStriped());
        BSwapGeneratorManager.get().registerGenerator(new BSGRandom());
        //BSwapGeneratorManager.get().registerGenerator(new BSGRandomStriped());


        // -- Kits --

        KitGroup blockSwapKits = new KitGroup("blockswap", "BlockSwap", true, new DefaultKit(), new LeaperKit(), new RunnerKit());
        NewGamesAPI1.getKitRegistry().registerKitGroup(blockSwapKits);

        // -- Generate game properties + meta --

        String[] gameMapCategoryTypes = {
                "blockswap",
                "bswap",
                "bswap2"
        };

        GameProperties gameProperties = new GameProperties(
                AutomaticWinPolicy.OPPOSING_PLAYERS_DEAD
        )
                .setGuidelinePlayers(BlockSwapConstants.MINIMUM_PLAYERS)
                .setMinimumPlayers(BlockSwapConstants.MINIMUM_PLAYERS)
                .setMaximumPlayers(BlockSwapConstants.MAXIMUM_PLAYERS)
                .setCanWorldBeManipulated(false)
                .setDefaultCountdownLength(10);

        GameProperties gameDebugProperties = new GameProperties(
                AutomaticWinPolicy.MANUAL_CALLS_ONLY
        )
                .setGuidelinePlayers(1)
                .setMinimumPlayers(1)
                .setMaximumPlayers(BlockSwapConstants.MAXIMUM_PLAYERS)
                .setCanWorldBeManipulated(false)
                .setDefaultCountdownLength(5);

        GameProperties gameTourneyProperties = new GameProperties(
                AutomaticWinPolicy.OPPOSING_PLAYERS_DEAD
        )
                .setGuidelinePlayers(1)
                .setMinimumPlayers(1)
                .setMaximumPlayers(BlockSwapConstants.MAXIMUM_PLAYERS_TOURNEY)
                .setCanWorldBeManipulated(false)
                .setTourneyGamemode(true)
                .setDefaultCountdownLength(10);

        GameProperties practiceProperties = new GameProperties(
                AutomaticWinPolicy.MANUAL_CALLS_ONLY
        )
                .setGuidelinePlayers(1)
                .setMinimumPlayers(1)
                .setMaximumPlayers(BlockSwapConstants.MAXIMUM_PLAYERS)
                .setCanWorldBeManipulated(false)
                .setDefaultCountdownLength(10)
                .setInternalRewardsEnabled(false);

        GameID game = new GameID(
                "blockswap",
                "bswap",
                "Block Swap",
                "Quick! Stand on the block displayed or you lose!",
                blockSwapKits.getGroupID(),
                gameMapCategoryTypes,
                2,
                gameProperties,
                BlockSwapGameBehaviour.class
        );

        GameID debug = new GameID(
                "debug_blockswap",
                "bswap",
                "Block Swap",
                "Quick! Stand on the block displayed or you lose!",
                blockSwapKits.getGroupID(),
                gameMapCategoryTypes,
                2,
                gameDebugProperties,
                BlockSwapGameBehaviour.class
        );

        GameID tourney = new GameID(
                "tourney_blockswap",
                "bswap",
                "Block Swap Tourney",
                "Quick! Stand on the block displayed or you lose! Points are awarded for survival + winning!",
                blockSwapKits.getGroupID(),
                gameMapCategoryTypes,
                2,
                gameTourneyProperties,
                BlockSwapGameBehaviour.class
        );

        GameID practice = new GameID(
                "practice_blockswap",
                "bswap",
                "Block Swap Practice",
                "Quick! Stand on the block displayed or you lose!",
                blockSwapKits.getGroupID(),
                gameMapCategoryTypes,
                2,
                practiceProperties,
                PracticeBlockSwapGameBehaviour.class
        );


        // -- Register game types --

        NewGamesAPI1.getGameRegistry()
                .registerGame(game)
                .registerGame(debug)
                .registerGame(tourney)
                .registerGame(practice);
    }

    public static BlockSwapPlugin get() { return instance; }
    public static BSwapGeneratorManager getGeneratorManager() { return get().generatorManager; }
    public static Logger getLog() { return get().getLogger(); }

}