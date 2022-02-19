package org.madblock.blockswap.utils;

import cn.nukkit.utils.DyeColor;
import cn.nukkit.utils.TextFormat;
import org.madblock.blockswap.powerups.*;
import org.madblock.blockswap.powerups.impl.*;

import java.util.*;

public class BlockSwapConstants {

    public static final int MINIMUM_PLAYERS = 2;
    public static final int MAXIMUM_PLAYERS = 16;

    public static final int ROUND_SECONDS = 6;

    public static final int POWERUP_SPAWN_TIMER_SECONDS = 20;
    public static final int MINIMUM_POWERUP_SPAWN_TIMER_SECONDS = 5;
    public static final int POWERUP_SPAWN_TIMER_SECONDS_DECREMENT = 2;

    public static final int MAXIMUM_POWERUPS_ON_MAP = 8;

    public static final float LEAP_STRENGTH = 1.7f;

    public static final int SCOREBOARD_COLOR_INDEX = 0;
    public static final int SCOREBOARD_PLAYERS_INDEX = 1;
    public static final int SCOREBOARD_ROUND_INDEX = 2;

    public static final String FUNCTION_TAG_GENERATE_PLATFORM = "platform_gen";
    public static final String FUNCTION_TAG_CLEAR_PLATFORM = "platform_clear";

    public static final Map<DyeColor, TextFormat> POSSIBLE_COLORS = Collections.unmodifiableMap(new HashMap<DyeColor, TextFormat>() {
        {
            this.put(DyeColor.RED, TextFormat.RED);
            this.put(DyeColor.BLACK, TextFormat.BLACK);
            this.put(DyeColor.BLUE, TextFormat.BLUE);
            this.put(DyeColor.CYAN, TextFormat.DARK_AQUA);
            this.put(DyeColor.GREEN, TextFormat.DARK_GREEN);
            this.put(DyeColor.LIGHT_BLUE, TextFormat.AQUA);
            this.put(DyeColor.LIME, TextFormat.GREEN);
            this.put(DyeColor.MAGENTA, TextFormat.LIGHT_PURPLE);
            this.put(DyeColor.ORANGE, TextFormat.GOLD);
            this.put(DyeColor.WHITE, TextFormat.WHITE);
            this.put(DyeColor.YELLOW, TextFormat.YELLOW);
        }
    });

    public static final List<Class<? extends PowerUp>> POSSIBLE_POWER_UPS = Collections.unmodifiableList(Arrays.asList(
            LeapPowerUp.class,
            TeleportPowerUp.class,
            ColorExchangePowerUp.class,
            BlindnessPowerUp.class,
            ShufflePowerUp.class,
            SpeedPowerUp.class));

}
