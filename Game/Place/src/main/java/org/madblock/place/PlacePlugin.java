package org.madblock.place;

import cn.nukkit.plugin.PluginBase;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.game.GameID;
import org.madblock.newgamesapi.game.GameProperties;
import org.madblock.newgamesapi.game.pvp.CustomGamePVPSettings;
import org.madblock.newgamesapi.kits.KitGroup;
import org.madblock.newgamesapi.registry.GameRegistry;
import org.madblock.newgamesapi.registry.KitRegistry;
import org.madblock.place.behavior.PlaceBehavior;
import org.madblock.place.kit.DefaultKit;

public class PlacePlugin extends PluginBase {

    private static PlacePlugin INSTANCE;


    @Override
    public void onEnable() {
        INSTANCE = this;

        GameProperties properties = new GameProperties(GameHandler.AutomaticWinPolicy.MANUAL_CALLS_ONLY)
                .setDefaultCountdownLength(0)
                .setCanPlayersMoveDuringCountdown(true)
                .setCustomPvpSettings(CustomGamePVPSettings.DISABLED)
                .setGuidelinePlayers(0)
                .setMinimumPlayers(0)
                .setMaximumPlayers(Integer.MAX_VALUE)
                .setItemDroppingEnabled(false)
                .setCanWorldBeManipulated(true)
                .setCanWorldBeManipulatedPreGame(true)
                .setFallDamageEnabled(false)
                .setTourneyGamemode(true);

        KitGroup group = new KitGroup("place",
                "Place Kits",
                false,
                new DefaultKit());

        GameID gameID = new GameID("place",
                "place",
                "Place",
                "Wow! Is that r/place but on Minecaft???",
                group.getGroupID(),
                new String[] { "place" },
                1,
                properties,
                PlaceBehavior.class);

        KitRegistry.get().registerKitGroup(group);
        GameRegistry.get().registerGame(gameID);
    }

    public static PlacePlugin get() {
        return INSTANCE;
    }

}
