package org.madblock.punishments.utils;

import cn.nukkit.utils.TextFormat;


// Copied from NewGamesAPI1. Punishments should not be dependent on the api.
public class Utility {

    public static final TextFormat DEFAULT_TEXT_COLOUR = TextFormat.GRAY;
    public static String generateServerMessage(String topic, TextFormat topicColour, String text){
        return generateServerMessage(topic, topicColour, text, DEFAULT_TEXT_COLOUR);
    }

    public static String generateServerMessage(String topic, TextFormat topicColour, String text, TextFormat defaultTextColour){
        return String.format("%s%s%s %s%s>> %s%s%s", topicColour, TextFormat.BOLD, topic, TextFormat.DARK_GRAY, TextFormat.BOLD, TextFormat.RESET, defaultTextColour, text);
    }

    public static String generateTimeLeftMessage (long time) {
        StringBuilder builder = new StringBuilder();
        long daysLeft = time / (60000 * 60 * 24);
        long hoursLeft = (time - (daysLeft * 60000 * 60 * 24)) / (60000 * 60);
        long minutesLeft = (time - (daysLeft * 60000 * 60 * 24) - (hoursLeft * 60000 * 60)) / (60000);
        long secondsLeft = (time - (daysLeft * 60000 * 60 * 24) - (hoursLeft * 60000 * 60) - (minutesLeft * 60000)) / 1000;

        if (daysLeft > 0) {
            builder.append(daysLeft).append(" days");
            if (hoursLeft > 0) {
                builder.append(" and ").append(hoursLeft).append(" hours");
            } else if (minutesLeft > 0) {
                builder.append(" and ").append(minutesLeft).append(" minutes");
            }
        } else if (hoursLeft > 0) {
            builder.append(hoursLeft).append(" hours");
            if (minutesLeft > 0) {
                builder.append(" and ").append(minutesLeft).append(" minutes");
            }
        } else if (minutesLeft > 0) {
            builder.append(minutesLeft).append(" minutes and ").append(secondsLeft).append(" seconds");
        } else {
            builder.append(secondsLeft).append(" seconds");
        }

        return builder.toString();
    }

}
