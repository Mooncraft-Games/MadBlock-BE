package org.madblock.crystalwars.game.pointentities.capturepoint;

import org.madblock.newgamesapi.util.Utility;
import org.madblock.newgamesapi.game.GameHandler;

public class MiddleCapturePointEntity extends CapturePointEntity {
    public static final String ID = "madblock_crystalwars_middlepoint";

    public MiddleCapturePointEntity(GameHandler gameHandler) {
        super(ID, gameHandler);
    }

    @Override
    public String getName() {
        return "the " + Utility.ResourcePackCharacters.DIAMOND;
    }
}