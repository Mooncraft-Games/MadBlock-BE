package org.madblock.gamemodesumox;

import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.util.Utility;

public class SumoXStrings {

    public static final String PANIC_TITLE = String.format("%s%s/!\\ %s%sPANIC MODE %s%s/!\\", TextFormat.DARK_RED, TextFormat.BOLD, TextFormat.RED, TextFormat.BOLD, TextFormat.DARK_RED, TextFormat.BOLD);
    public static final String PANIC_SUBTITILE = String.format("%s%sWarning! Knockback is increasing!", TextFormat.GOLD, TextFormat.BOLD);
    public static final String PANIC_MESSAGE = Utility.generateServerMessage("!!!", TextFormat.DARK_RED, String.format("Panic Mode! %sKnockback is increasing!", TextFormat.GOLD), TextFormat.RED);

    public static final String DEAD_TITLE = String.format("%s%sDEAD", TextFormat.DARK_GRAY, TextFormat.BOLD);
    public static final String DEAD_SUBTITLE = String.format("%sYou ran out of lives! :(", TextFormat.GRAY);

    public static final String POWERUP_ENTITY_NAME = String.format("%s%s^ POWER UP ^\n%s%s(In A Suit)", TextFormat.YELLOW, TextFormat.BOLD, TextFormat.BLUE, TextFormat.GOLD);
}
