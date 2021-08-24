package org.madblock.crystalwars.game.pointentities.capturepoint;

import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameHandler;

public class GoldCapturePointEntity extends CapturePointEntity {
    public static final String ID = "madblock_crystalwars_goldpoint";

    public GoldCapturePointEntity(GameHandler gameHandler) {
        super(ID, gameHandler);
    }

    @Override
    public String getName() {
        return "a " + Utility.ResourcePackCharacters.GOLD_INGOT;
    }
}