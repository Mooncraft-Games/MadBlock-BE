package org.madblock.ranks.util;

import cn.nukkit.utils.TextFormat;

public class Utility {

    public static final TextFormat DEFAULT_TEXT_COLOUR = TextFormat.GRAY;
    public static String generateServerMessage(String topic, TextFormat topicColour, String text){
        return generateServerMessage(topic, topicColour, text, DEFAULT_TEXT_COLOUR);
    }

    public static String generateServerMessage(String topic, TextFormat topicColour, String text, TextFormat defaultTextColour){
        return String.format("%s%s%s %s%s>> %s%s%s", topicColour, TextFormat.BOLD, topic, TextFormat.DARK_GRAY, TextFormat.BOLD, TextFormat.RESET, defaultTextColour, text);
    }

}