package org.madblock.towerwars;

import cn.nukkit.plugin.PluginBase;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.game.GameID;
import org.madblock.newgamesapi.game.GameProperties;
import org.madblock.newgamesapi.kits.KitGroup;
import org.madblock.newgamesapi.registry.GameRegistry;
import org.madblock.newgamesapi.registry.KitRegistry;
import org.madblock.towerwars.behaviors.TowerWarsSoloBehavior;
import org.madblock.towerwars.kits.DefaultKit;

public class TowerWarsPlugin extends PluginBase {

    private static TowerWarsPlugin instance;

    @Override
    public void onEnable() {

        KitGroup kitGroup = new KitGroup("towerwars", "Tower Wars", false, new DefaultKit());
        KitRegistry.get().registerKitGroup(kitGroup);

        GameProperties properties = new GameProperties(GameHandler.AutomaticWinPolicy.MANUAL_CALLS_ONLY)
                .setDefaultCountdownLength(3)
                .setCanPlayersMoveDuringCountdown(true)
                .setCanWorldBeManipulated(true)
                .setMinimumPlayers(1)
                .setMaximumPlayers(8)
                .setGuidelinePlayers(2);
        GameID game = new GameID(
                "towerwars",
                "tw",
                "TowerWars",
                "Purchase towers and summon monsters to attack your enemies!",
                "towerwars",
                new String[]{ "towerwars" },
                1,
                properties,
                TowerWarsSoloBehavior.class
        );
        GameRegistry.get().registerGame(game);

        instance = this;
    }

    public static TowerWarsPlugin get() {
        return instance;
    }

}