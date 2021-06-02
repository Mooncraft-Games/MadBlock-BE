package org.madblock.skywars.utils;

import org.madblock.skywars.powerups.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Constants {

    public static final String POWERUP_ITEM_NBT_ID = "skywars_NBT_powerup_id";

    public static final String COMPASS_ITEM_NBT_ID = "skywars_NBT_compass";

    public static final String GAME_ID = "skywars";

    public static final List<String> GAME_MAP_CATEGORY_TYPES = Collections.unmodifiableList(Arrays.asList("skywars"));

    public static final List<Class<? extends PowerUp>> POSSIBLE_POWER_UPS = Collections.unmodifiableList(Arrays.asList(
            TNTPowerUp.class,
            LeapPowerUp.class,
            EscapePowerUp.class,
            GiantSnowBallPowerUp.class,
            DelusionalPowerUp.class
    ));

    public static final int MINIMUM_PLAYERS = 2;

    public static final int MAXIMUM_PLAYERS = 16;

    public static final int DEFAULT_CORRUPTION_TIME = 60 * 2;

    public static final int POWERUP_SPAWN_COOLDOWN = 30;

    public static final int CORRUPTION_SPEED = 6;

    public static final int DEFAULT_GRACE_PERIOD = 5;

    public static final int DEFAULT_CORRUPTION_RADIUS = 100;

    public static final float LEAP_STRENGTH = 1.55f;

    public static final int SCOREBOARD_PLAYERS_INDEX = 0;
    public static final int SCOREBOARD_KILLS_INDEX = 1;
    public static final int SCOREBOARD_CORRUPTION_INDEX = 2;

}
