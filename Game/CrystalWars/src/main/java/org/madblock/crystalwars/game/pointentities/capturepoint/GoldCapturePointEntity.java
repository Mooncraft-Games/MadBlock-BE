package org.madblock.crystalwars.game.pointentities.capturepoint;

import org.madblock.newgamesapi.game.GameHandler;

/**
 * @author Nicholas
 */
public class GoldCapturePointEntity extends CapturePointEntity {
    public static final String ID = "madblock_crystalwars_goldpoint";

    public GoldCapturePointEntity(GameHandler gameHandler) {
        super(ID, gameHandler);
    }

    @Override
    public String getName() {
        return "a Gold";
    }
}