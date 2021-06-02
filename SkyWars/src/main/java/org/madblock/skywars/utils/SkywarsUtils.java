package org.madblock.skywars.utils;

import cn.nukkit.block.Block;
import cn.nukkit.block.BlockNetherrack;
import cn.nukkit.block.BlockSoulSand;
import org.madblock.skywars.powerups.PowerUp;

public class SkywarsUtils {

    public static Class<? extends PowerUp> getRandomPowerUp () {
        return Constants.POSSIBLE_POWER_UPS.get((int)Math.floor(Math.random() * Constants.POSSIBLE_POWER_UPS.size()));
    }

    public static Block getRandomCorruptionBlock () {
        if (Math.random() > 0.25f) {
            return new BlockNetherrack();
        } else {
            return new BlockSoulSand();
        }
    }


}
