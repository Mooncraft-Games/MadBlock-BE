package org.madblock.crystalwars;

import cn.nukkit.entity.Entity;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginBase;
import lombok.Getter;
import org.madblock.crystalwars.game.CrystalWarsGame;
import org.madblock.crystalwars.game.entities.EntityHumanCrystal;
import org.madblock.crystalwars.game.kit.WarriorKit;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.game.GameID;
import org.madblock.newgamesapi.game.GameProperties;
import org.madblock.newgamesapi.kits.KitGroup;
import org.madblock.newgamesapi.util.NGAPIConstants;

public class CrystalWarsPlugin extends PluginBase {
    @Getter private static Plugin instance;

    @Override
    public void onEnable() {
        instance = this;

        KitGroup kitGroup = new KitGroup("crystalwars", "Crystal Wars", true, new WarriorKit());

        GameProperties regularCrystalWarsProperties = new GameProperties(GameHandler.AutomaticWinPolicy.MANUAL_CALLS_ONLY)
                .setCanWorldBeManipulated(true)
                .setDoesGameShufflePlayerSpawns(false)
                .setItemDroppingEnabled(true)
                .setItemPickUpEnabled(true)
                .setFallDamageEnabled(true)
                .setGuidelinePlayers(2)
                .setMaximumPlayers(16)
                .setMinimumPlayers(2)
                .setDefaultCountdownLength(15);

        GameProperties tourneyCrystalWarsProperties = new GameProperties(GameHandler.AutomaticWinPolicy.MANUAL_CALLS_ONLY)
                .setCanWorldBeManipulated(true)
                .setDoesGameShufflePlayerSpawns(false)
                .setItemDroppingEnabled(true)
                .setItemPickUpEnabled(true)
                .setFallDamageEnabled(true)
                .setGuidelinePlayers(2)
                .setMaximumPlayers(4)
                .setMinimumPlayers(1)
                .setDefaultCountdownLength(15)
                .setTourneyGamemode(true);

        NewGamesAPI1.getGameRegistry().registerGame(new GameID(CrystalWarsConstants.BASE_GAME_ID, CrystalWarsConstants.BASE_GAME_SERVER_ID,
                "Crystal Wars Squads", "Destroy the other crystals, but make sure to defend your own!",
                "crystalwars", new String[] { "crystalwars_regular" }, 2, regularCrystalWarsProperties, CrystalWarsGame.class));

        NewGamesAPI1.getGameRegistry().registerGame(new GameID(CrystalWarsConstants.TOURNEY_GAME_ID, NGAPIConstants.EVENT_SERVER_ID,
                "Crystal Wars Tourney", "Destroy the other crystals, but make sure to defend your own!",
                "crystalwars", new String[] { "crystalwars_regular" }, 2, tourneyCrystalWarsProperties, CrystalWarsGame.class));

        NewGamesAPI1.getKitRegistry().registerKitGroup(kitGroup);

        Entity.registerEntity("human_crystal", EntityHumanCrystal.class);
        EntityHumanCrystal.getCrystalSkin("yellow"); // Pre-load the default team skin.
        EntityHumanCrystal.getCrystalSkin("red"); //    ^
        EntityHumanCrystal.getCrystalSkin("blue"); //   ^
        EntityHumanCrystal.getCrystalSkin("green"); //  ^
        EntityHumanCrystal.getCrystalSkin("purple"); // Pre-load the repair skin.
    }
}