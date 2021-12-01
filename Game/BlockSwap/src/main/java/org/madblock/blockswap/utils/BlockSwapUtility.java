package org.madblock.blockswap.utils;

import cn.nukkit.utils.DyeColor;
import cn.nukkit.utils.TextFormat;
import org.madblock.blockswap.powerups.PowerUp;

import java.util.ArrayList;
import java.util.List;

public class BlockSwapUtility {

    /**
     * Retrieve random platform colors.
     * If there are not enough colors then it will default to the maximum amount of colors.
     * @param maximumAmount maximum amount of colors
     */
    public static List<DyeColor> getRandomColors(int maximumAmount) {
        List<DyeColor> colors = new ArrayList<>(BlockSwapConstants.POSSIBLE_COLORS.keySet());
        int requiredColors = maximumAmount;
        if (requiredColors > colors.size()) {
            requiredColors = colors.size();
        }

        while (colors.size() != requiredColors) {
            colors.remove((int) Math.floor(Math.random() * colors.size()));
        }

        return colors;
    }

    public static Class<? extends PowerUp> getRandomPowerUp() {
        return BlockSwapConstants.POSSIBLE_POWER_UPS.get((int) Math.floor(Math.random() * BlockSwapConstants.POSSIBLE_POWER_UPS.size()));
    }

    public static String getPowerUpItemName(DyeColor color, PowerUp powerUp) {
        return new StringBuilder()
                .append(getBlockItemName(color))
                .append('\n')
                .append(TextFormat.RESET)
                .append(TextFormat.GREEN)
                .append("Tap to use ")
                .append(TextFormat.BOLD)
                .append(TextFormat.YELLOW)
                .append(powerUp.getName())
                .append(TextFormat.RESET)
                .append(TextFormat.GREEN)
                .append("!")
                .toString();
    }

    public static String getBlockItemName(DyeColor color) {
        return "" + BlockSwapConstants.POSSIBLE_COLORS.getOrDefault(color, TextFormat.WHITE) + TextFormat.BOLD + color.getName();
    }

}
