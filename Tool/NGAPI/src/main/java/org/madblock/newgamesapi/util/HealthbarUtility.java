package org.madblock.newgamesapi.util;

import cn.nukkit.utils.TextFormat;

import java.text.DecimalFormat;

/**
 * Ported from https://github.com/CloudG360/OofTracker
 * @author CG360
 */
public class HealthbarUtility {

    public static final DecimalFormat HEALTH_FORMAT = new DecimalFormat("0.0");
    public static final double THRESHOLD_HEALTHY = 0.85d; // > = green
    public static final double THRESHOLD_OKAY = 0.6d; // > = yellow
    public static final double THRESHOLD_WOUNDED = 0.25d; // > = orange | < = red

    public enum HealthbarType {
        TEXT_MONO, TEXT_SPLIT, TEXT,
        BAR_MONO, BAR_MONO_NO_TEXT, BAR_DUO, BAR_DUO_NO_TEXT, BAR, BAR_NO_TEXT,
        SQUARES_MONO, SQUARES_MONO_NO_TEXT, SQUARES, SQUARES_NO_TEXT
    }




    public static TextFormat getHealthColour(double health, double maxHealth) {
        double checkedMaxHealth = maxHealth > 0 ? maxHealth : 1; // Ensure maxHealth is not 0.
        double fraction = health / checkedMaxHealth;

        if (fraction >= THRESHOLD_HEALTHY) return TextFormat.GREEN;
        if (fraction >= THRESHOLD_OKAY) return TextFormat.YELLOW;
        if(fraction >= THRESHOLD_WOUNDED) return TextFormat.GOLD;

        return TextFormat.RED; // Otherwise it's red cause it's below the threshold
    }

    public static String getHealthText(HealthbarType type, double health, double maxHealth) {

        switch (type) {

            // -- TEXT --

            case TEXT_MONO:
                return genTextFormat(TextFormat.RED, TextFormat.DARK_RED, health, maxHealth);

            case TEXT_SPLIT:
                return genTextFormat(null, TextFormat.DARK_RED, health, maxHealth);

            case TEXT:
                return genTextFormat(null, null, health, maxHealth);


            // -- BAR --

            case BAR_MONO_NO_TEXT:
                return genBarFormat(TextFormat.RED, null, health, maxHealth, false);
            case BAR_MONO:
                return genBarFormat(TextFormat.RED, null, health, maxHealth, true);
            case BAR_DUO_NO_TEXT:
                return genBarFormat(TextFormat.GREEN, TextFormat.RED, health, maxHealth, false);
            case BAR_DUO:
                return genBarFormat(TextFormat.GREEN, TextFormat.RED, health, maxHealth, true);
            case BAR_NO_TEXT:
                return genBarFormat(null, null, health, maxHealth, false);
            case BAR:
                return genBarFormat(null, null, health, maxHealth, true);



            // -- SQUARES --

            case SQUARES_MONO_NO_TEXT:
                return genSquaresFormat(TextFormat.RED, health, maxHealth, false);
            case SQUARES_MONO:
                return genSquaresFormat(TextFormat.RED, health, maxHealth, true);
            case SQUARES_NO_TEXT:
                return genSquaresFormat(null, health, maxHealth, false);

            default: // SQUARES is default.
            case SQUARES:
                return genSquaresFormat(null, health, maxHealth, true);
        }
    }

    /**
     * Generates the text string for "TEXT" variety health bars.
     * @param primary the colour of the health. Colour is based on health if null.
     * @param secondary the colour of the max health. Colour is based on health if null.
     * @param health the entity's health.
     * @param maxHealth the entity's max health
     * @return the built Raw Text string.
     */
    private static String genTextFormat(TextFormat primary, TextFormat secondary, double health, double maxHealth) {
        String healthString = HEALTH_FORMAT.format(health);
        String maxHealthString = HEALTH_FORMAT.format(maxHealth);
        TextFormat genColour = getHealthColour(health, maxHealth);

        return new RawTextBuilder(healthString).setBold(true).setColor(primary == null ? genColour : primary)
                .append(new RawTextBuilder(String.format(" / %s \u2661", maxHealthString)).setColor(secondary == null ? genColour : secondary))
                .toString();
    }

    /**
     * Generates the text string for "SQUARES" variety health bars.
     * @param primary the colour of the health bar. Colour is based on health if null.
     * @param health the entity's health.
     * @param maxHealth the entity's max health
     * @return the built Raw Text string.
     */
    private static String genSquaresFormat(TextFormat primary, double health, double maxHealth, boolean includeText) {
        String healthString = HEALTH_FORMAT.format(health);
        String maxHealthString = HEALTH_FORMAT.format(maxHealth);
        TextFormat barColour = primary == null ? getHealthColour(health, maxHealth) : primary;

        double checkedMaxHealth = maxHealth > 0 ? maxHealth : 1; // Ensure maxHealth is not 0.
        double healthFraction = health / checkedMaxHealth;

        RawTextBuilder barBuilder = new RawTextBuilder().setBold(false).setColor(barColour);

        for(double i = 0; i < 1; i += 0.1d) { // Tbh this could be split into two RawText components rather than 10.
            RawTextBuilder squareBuilder = new RawTextBuilder("\u25A0");
            if(healthFraction < i) squareBuilder.setColor(TextFormat.GRAY); // Override base colour if true
            barBuilder.append(squareBuilder);
        }

        if(includeText) {
            RawTextBuilder fullBuilder = new RawTextBuilder().setBold(true).setColor(barColour);
            // Surround the bar text.
            fullBuilder.append(new RawTextBuilder(healthString + " "));
            fullBuilder.append(barBuilder);
            fullBuilder.append(new RawTextBuilder(" " + maxHealthString));
            return fullBuilder.toString();

        } else {
            return barBuilder.toString(); // The bar will be enough.
        }
    }

    /**
     * Generates the text string for "SQUARES" variety health bars.
     * @param primary the colour of the health bar. Colour is based on health if null.
     * @param health the entity's health.
     * @param maxHealth the entity's max health
     * @return the built Raw Text string.
     */
    private static String genBarFormat(TextFormat primary, TextFormat secondary, double health, double maxHealth, boolean includeText) {
        String healthString = HEALTH_FORMAT.format(health);
        String maxHealthString = HEALTH_FORMAT.format(maxHealth);
        TextFormat barColour = primary == null ? getHealthColour(health, maxHealth) : primary;
        TextFormat barEmptyColour = secondary == null ? TextFormat.GRAY : secondary;

        double checkedMaxHealth = maxHealth > 0 ? maxHealth : 1; // Ensure maxHealth is not 0.
        double healthFraction = health / checkedMaxHealth;

        RawTextBuilder barBuilder = new RawTextBuilder().setBold(false).setColor(barColour);

        barBuilder.append(new RawTextBuilder("[").setColor(TextFormat.DARK_GRAY));

        for(double i = 0; i < 1; i += 0.05d) { // Tbh this could be split into two RawText components rather than 10.
            RawTextBuilder squareBuilder = new RawTextBuilder("|");
            if(healthFraction < i) squareBuilder.setColor(barEmptyColour); // Override base colour if true
            barBuilder.append(squareBuilder);
        }

        barBuilder.append(new RawTextBuilder("]").setColor(TextFormat.DARK_GRAY));

        if(includeText) {
            RawTextBuilder fullBuilder = new RawTextBuilder().setBold(true).setColor(barColour);
            // Surround the bar text.
            fullBuilder.append(new RawTextBuilder(healthString + " "));
            fullBuilder.append(barBuilder);
            fullBuilder.append(new RawTextBuilder(" " + maxHealthString).setColor(secondary == null ? barColour : TextFormat.RED));
            return fullBuilder.toString();

        } else {
            return barBuilder.toString(); // The bar will be enough.
        }
    }

}
