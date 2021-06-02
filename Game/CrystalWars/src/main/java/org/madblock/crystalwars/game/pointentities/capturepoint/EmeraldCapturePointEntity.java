package org.madblock.crystalwars.game.pointentities.capturepoint;

import org.madblock.newgamesapi.game.GameHandler;

/**
 * @author Nicholas
 */
public class EmeraldCapturePointEntity extends CapturePointEntity {
    public static final String ID = "madblock_crystalwars_emeraldpoint";

    public EmeraldCapturePointEntity(GameHandler gameHandler) {
        super(ID, gameHandler);
    }

    @Override
    public String getName() {
        return "an Emerald";
    }
}