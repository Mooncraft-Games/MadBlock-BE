package org.madblock.gamemodesumox;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.PluginLogger;
import org.madblock.gamemodesumox.games.GBehaveSumoBase;
import org.madblock.gamemodesumox.kits.KitArcher;
import org.madblock.gamemodesumox.kits.KitRunner;
import org.madblock.gamemodesumox.kits.KitSlapper;
import org.madblock.gamemodesumox.kits.KitTimelord;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.game.GameID;
import org.madblock.newgamesapi.game.GameProperties;
import org.madblock.newgamesapi.kits.KitGroup;
import org.madblock.newgamesapi.registry.GameRegistry;
import org.madblock.newgamesapi.registry.KitRegistry;

public class SumoX extends PluginBase {

    public static SumoX sumoxinstance;

    @Override
    public void onEnable() {
        sumoxinstance = this;

        KitRegistry.get().registerKitGroup(new KitGroup("sumox", "Sumo X", true, new KitSlapper(), new KitRunner(), new KitTimelord(), new KitArcher()));

        GameProperties sumoBrawlProperties = new GameProperties(GameHandler.AutomaticWinPolicy.MANUAL_CALLS_ONLY)
                .setCanPlayersMoveDuringCountdown(false)
                .setCanWorldBeManipulated(false)
                .setDefaultCountdownLength(10)
                .setMinimumPlayers(2)
                .setGuidelinePlayers(2)
                .setMaximumPlayers(4);
        GameProperties sumoMegaProperties = new GameProperties(GameHandler.AutomaticWinPolicy.MANUAL_CALLS_ONLY)
                .setCanPlayersMoveDuringCountdown(false)
                .setCanWorldBeManipulated(false)
                .setDefaultCountdownLength(10)
                .setMinimumPlayers(4)
                .setGuidelinePlayers(8)
                .setMaximumPlayers(32);

        GameProperties sumoBrawlTourneyProperties = new GameProperties(GameHandler.AutomaticWinPolicy.MANUAL_CALLS_ONLY)
                .setCanPlayersMoveDuringCountdown(false)
                .setCanWorldBeManipulated(false)
                .setDefaultCountdownLength(10)
                .setMinimumPlayers(1)
                .setGuidelinePlayers(1)
                .setMaximumPlayers(4)
                .setTourneyGamemode(true);
        GameProperties sumoMegaTourneyProperties = new GameProperties(GameHandler.AutomaticWinPolicy.MANUAL_CALLS_ONLY)
                .setCanPlayersMoveDuringCountdown(false)
                .setCanWorldBeManipulated(false)
                .setDefaultCountdownLength(10)
                .setMinimumPlayers(1)
                .setGuidelinePlayers(1)
                .setMaximumPlayers(32)
                .setTourneyGamemode(true);

        GameID sumoBrawlID = new GameID("sumox_brawl", "sumobrawl", "Sumo X Brawl", "The all new Sumo! Slap up to 4 players off the platform till they run out of lives! The last standing wins each round!", "sumox", new String[]{"sumo", "sumox"}, 2, sumoBrawlProperties, GBehaveSumoBase.class);
        GameID sumoMegaID = new GameID("sumox_mega", "sumomega", "Sumo X Mega", "The all new Sumo! Slap up to 32 players off the platform till they run out of lives! The last standing wins each round!", "sumox", new String[]{"sumo", "sumox"}, 2, sumoMegaProperties, GBehaveSumoBase.class);

        GameID tourneySumoBrawlID = new GameID("tourney_sumox_brawl", "sumobrawl", "Sumo X Mega Tourney", "The all new Sumo! Slap up to 4 players off the platform till they run out of lives! The last standing wins each round! Kills, Wins, and the top 3 positions are worth points!", "sumox", new String[]{"sumo", "sumox"}, 2, sumoBrawlTourneyProperties, GBehaveSumoBase.class);
        GameID tourneySumoMegaID = new GameID("tourney_sumox_mega", "sumomega", "Sumo X Mega Tourney", "The all new Sumo! Slap up to 32 players off the platform till they run out of lives! The last standing wins each round! Kills, Wins, and the top 3 positions are worth points!", "sumox", new String[]{"sumo", "sumox"}, 2, sumoMegaTourneyProperties, GBehaveSumoBase.class);


        GameRegistry.get()
                .registerGame(sumoBrawlID)
                .registerGame(sumoMegaID)
                .registerGame(tourneySumoBrawlID)
                .registerGame(tourneySumoMegaID)
        ;
    }

    public static SumoX get() { return sumoxinstance; }
    public static PluginLogger getPlgLogger(){ return get().getLogger(); }
}
