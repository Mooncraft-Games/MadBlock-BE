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
     * @param maximumAmount
     */
    public static List<DyeColor> getRandomColors (int maximumAmount) {
        List<DyeColor> colors = new ArrayList<>(BlockSwapConstants.POSSIBLE_COLORS.keySet());
        int requiredColors = maximumAmount;
        if (requiredColors > colors.size()) {
            requiredColors = colors.size();
        }

        while (colors.size() != requiredColors) {
            colors.remove((int)Math.floor(Math.random() * colors.size()));
        }

        return colors;
    }

    /**
     * Return a random platform color to use.
     * The "winning" color has a lower chance of being chosen the further the game is.
     * @param possibleColors Possible colors to choose from i
     * @param winningColor Winning color.
     * @param completedRounds Amount of rounds that were completed.
     */
    public static DyeColor getRandomColor (List<DyeColor> possibleColors, DyeColor winningColor, int completedRounds) {
        DyeColor randomColor = possibleColors.get((int)Math.floor(Math.random() * possibleColors.size()));
        if (randomColor.getWoolData() == winningColor.getWoolData() && Math.random() > Math.max((1.0 - (completedRounds * 0.05)), 0.1)) {
            // Limit it depending on the round.
            return getRandomColor(possibleColors, winningColor, completedRounds);
        }
        return randomColor;
    }

    public static Class<? extends PowerUp> getRandomPowerUp () {
        return BlockSwapConstants.POSSIBLE_POWER_UPS.get((int)Math.floor(Math.random() * BlockSwapConstants.POSSIBLE_POWER_UPS.size()));
    }

    public static String getPowerUpItemName (PowerUp powerUp) {
        return String.format("%sTap to use your %s%s%s%s%s power up!", TextFormat.GREEN, TextFormat.BOLD, TextFormat.YELLOW, powerUp.getName(), TextFormat.RESET, TextFormat.GREEN);
    }

}
