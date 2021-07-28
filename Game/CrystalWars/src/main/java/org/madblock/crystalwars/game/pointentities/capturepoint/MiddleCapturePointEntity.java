package org.madblock.crystalwars.game.pointentities.capturepoint;

import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameHandler;

/**
 * @author Nicholas
 */
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