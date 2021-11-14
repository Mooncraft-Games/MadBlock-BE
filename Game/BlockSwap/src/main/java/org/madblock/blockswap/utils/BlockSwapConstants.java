package org.madblock.blockswap.utils;

import cn.nukkit.utils.DyeColor;
import cn.nukkit.utils.TextFormat;
import org.madblock.blockswap.powerups.*;

import java.util.*;

public class BlockSwapConstants {

    public static final int COLORS_TO_BE_USED = 6;

    public static final int MINIMUM_PLAYERS = 2;
    public static final int MAXIMUM_PLAYERS = 16;

    public static final int ROUND_SECONDS = 8;

    public static final int POWERUP_SPAWN_TIMER_SECONDS = 20;
    public static final int MINIMUM_POWERUP_SPAWN_TIMER_SECONDS = 5;
    public static final int POWERUP_SPAWN_TIMER_SECONDS_DECREMENT = 2;

    public static final int MAXIMUM_POWERUPS_ON_MAP = 8;

    public static final int MINIMUM_COLOR_SCALE = 1;
    public static final int MAXIMUM_COLOR_SCALE = 4;

    public static final float LEAP_STRENGTH = 1.7f;

    public static final int SCOREBOARD_COLOR_INDEX = 0;
    public static final int SCOREBOARD_TIME_INDEX = 1;
    public static final int SCOREBOARD_PLAYERS_INDEX = 2;
    public static final int SCOREBOARD_POWERUP_INDEX = 3;

    public static final String[] GAME_MAP_CATEGORIES = new String[] {
            "blockswap"
    };

    public static final Map<DyeColor, TextFormat> POSSIBLE_COLORS;

    public static final List<Class<? extends PowerUp>> POSSIBLE_POWER_UPS;


    static {
        HashMap<DyeColor, TextFormat> possibleColourPrepare = new HashMap<>();
        possibleColourPrepare.put(DyeColor.RED, TextFormat.RED);
        possibleColourPrepare.put(DyeColor.BLACK, TextFormat.BLACK);
        possibleColourPrepare.put(DyeColor.BLUE, TextFormat.BLUE);
        // possibleColourPrepare.put(DyeColor.BROWN, TextFormat.DARK_RED); //?!?!
        possibleColourPrepare.put(DyeColor.CYAN, TextFormat.DARK_AQUA);
        possibleColourPrepare.put(DyeColor.GRAY, TextFormat.DARK_GRAY);
        possibleColourPrepare.put(DyeColor.GREEN, TextFormat.DARK_GREEN);
        possibleColourPrepare.put(DyeColor.LIGHT_BLUE, TextFormat.AQUA);
        possibleColourPrepare.put(DyeColor.LIGHT_GRAY, TextFormat.GRAY);
        possibleColourPrepare.put(DyeColor.LIME, TextFormat.GREEN);
        possibleColourPrepare.put(DyeColor.MAGENTA, TextFormat.LIGHT_PURPLE);
        possibleColourPrepare.put(DyeColor.ORANGE, TextFormat.GOLD);
        // possibleColourPrepare.put(DyeColor.PINK, TextFormat.RED);
        possibleColourPrepare.put(DyeColor.WHITE, TextFormat.WHITE);
        possibleColourPrepare.put(DyeColor.YELLOW, TextFormat.YELLOW);

        POSSIBLE_COLORS = Collections.unmodifiableMap(possibleColourPrepare);

        List<Class<? extends PowerUp>> possiblePowerUps = new ArrayList<>();

        possiblePowerUps.add(LeapPowerUp.class);
        possiblePowerUps.add(TeleportPowerUp.class);
        possiblePowerUps.add(ColorExchangePowerUp.class);
        possiblePowerUps.add(BlindnessPowerUp.class);
        possiblePowerUps.add(SlownessPowerUp.class);
        possiblePowerUps.add(ExplodePowerUp.class);
        possiblePowerUps.add(AntidotePowerUp.class);

        POSSIBLE_POWER_UPS = Collections.unmodifiableList(possiblePowerUps);
    }

}
