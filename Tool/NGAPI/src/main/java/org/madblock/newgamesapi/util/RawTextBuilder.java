package org.madblock.newgamesapi.util;

import cn.nukkit.utils.TextFormat;

import java.util.ArrayList;

/**
 * A utility class that builds chains of formatted strings
 * together. It can either export a JSON raw text string or
 * a formatted Nukkit string.
 *
 * Ported from https://github.com/CloudG360/OofTracker
 * @author CG360
 */
public class RawTextBuilder {

    protected ArrayList<RawTextBuilder> extra;

    protected String text;
    protected String color;

    protected Boolean bold;
    protected Boolean italic;
    protected Boolean underlined;
    protected Boolean strikethrough;
    protected Boolean obfuscated;


    public RawTextBuilder() { this(null); }
    public RawTextBuilder(String text) {
        this.text = text;
        this.color = null;

        this.bold = null;
        this.italic = null;
        this.underlined = null;
        this.strikethrough = null;
        this.obfuscated = null;
    }

    /**
     * Adds a component to the Raw Text's "extra" tag. Creates a list
     * if one is not already present.
     * @param extraComponent the component to add.
     * @return this for chaining
     */
    public RawTextBuilder append(RawTextBuilder extraComponent) {

        if(this.extra == null) {
            this.extra = new ArrayList<>();

        }

        this.extra.add(extraComponent);
        return this;
    }



    public ArrayList<RawTextBuilder> getExtra() { return extra; }
    public String getText() { return text; }
    public String getColor() { return color; }
    public Boolean isBold() { return bold; }
    public Boolean isItalic() { return italic; }
    public Boolean isUnderlined() { return underlined; }
    public Boolean isStrikethrough() { return strikethrough; }
    public Boolean isObfuscated() { return obfuscated; }



    public RawTextBuilder setExtra(ArrayList<RawTextBuilder> extra) { this.extra = extra; return this; }
    public RawTextBuilder setText(String text) { this.text = text; return this; }
    public RawTextBuilder setColor(TextFormat color) { this.color = color.name().toLowerCase(); return this; } // Change if the names change.
    public RawTextBuilder setColor(String color) { this.color = color; return this; }
    public RawTextBuilder setBold(Boolean bold) { this.bold = bold; return this; }
    public RawTextBuilder setItalic(Boolean italic) { this.italic = italic; return this; }
    public RawTextBuilder setUnderlined(Boolean underlined) { this.underlined = underlined; return this; }
    public RawTextBuilder setStrikethrough(Boolean strikethrough) { this.strikethrough = strikethrough; return this; }
    public RawTextBuilder setObfuscated(Boolean obfuscated) { this.obfuscated = obfuscated; return this; }


    /** @return a built RawText JSON string. */
    public String toJsonString() {
        StringBuilder jsonTextBuilder = new StringBuilder();
        // Add a comma after every finished component. It'll be sorted at the end.
        jsonTextBuilder.append("{");

        if(extra != null) {
            jsonTextBuilder.append("\"extra\":[");
            boolean isEmpty = true;

            for(RawTextBuilder builder: extra) {

                if(builder != null) {
                    jsonTextBuilder.append(builder.toString()).append(",");
                    isEmpty = false;
                }
            }

            if(!isEmpty) jsonTextBuilder.deleteCharAt(jsonTextBuilder.length() - 1); // Delete last comma
            jsonTextBuilder.append("],");
        }

        if(text == null) {
            jsonTextBuilder.append("\"text\":\"\"").append(",");

        } else {
            jsonTextBuilder.append(String.format("\"text\":\"%s\"", text)).append(",");
        }


        if(color != null) jsonTextBuilder.append(String.format("\"color\":\"%s\"", color)).append(",");

        if(bold != null) jsonTextBuilder.append(String.format("\"bold\":%s", bold)).append(",");
        if(italic != null) jsonTextBuilder.append(String.format("\"italic\":%s", italic)).append(",");
        if(underlined != null) jsonTextBuilder.append(String.format("\"underlined\":%s", underlined)).append(",");
        if(strikethrough != null) jsonTextBuilder.append(String.format("\"strikethrough\":%s", strikethrough)).append(",");
        if(obfuscated != null) jsonTextBuilder.append(String.format("\"bold\":%s", obfuscated)).append(",");


        if(jsonTextBuilder.length() > 1) { // More than just the first bracket. Probably got a comma.
            jsonTextBuilder.deleteCharAt(jsonTextBuilder.length() - 1);
        }

        jsonTextBuilder.append("}");

        return jsonTextBuilder.toString();
    }


    protected StringBuilder addFormat(StringBuilder worker) {

        if(color != null) {
            switch (color.toUpperCase()) {
                case "BLACK":
                    worker.append(TextFormat.BLACK);
                    break;

                case "DARK_BLUE":
                    worker.append(TextFormat.DARK_BLUE);
                    break;

                case "DARK_GREEN":
                    worker.append(TextFormat.DARK_GREEN);
                    break;

                case "DARK_AQUA":
                    worker.append(TextFormat.DARK_AQUA);
                    break;

                case "DARK_RED":
                    worker.append(TextFormat.DARK_RED);
                    break;

                case "DARK_PURPLE":
                    worker.append(TextFormat.DARK_PURPLE);
                    break;

                case "GOLD":
                    worker.append(TextFormat.GOLD);
                    break;

                case "GREY":
                case "GRAY":
                    worker.append(TextFormat.GRAY);
                    break;

                case "DARK_GREY":
                case "DARK_GRAY":
                    worker.append(TextFormat.DARK_GRAY);
                    break;

                case "BLUE":
                    worker.append(TextFormat.BLUE);
                    break;

                case "GREEN":
                    worker.append(TextFormat.GREEN);
                    break;

                case "AQUA":
                    worker.append(TextFormat.AQUA);
                    break;

                case "RED":
                    worker.append(TextFormat.RED);
                    break;

                case "LIGHT_PURPLE":
                    worker.append(TextFormat.LIGHT_PURPLE);
                    break;

                case "YELLOW":
                    worker.append(TextFormat.YELLOW);
                    break;

                case "WHITE":
                default:
                    worker.append(TextFormat.WHITE);
            }
        }

        if(bold) worker.append(TextFormat.BOLD);
        if(italic) worker.append(TextFormat.ITALIC);
        if(underlined) worker.append(TextFormat.UNDERLINE);
        if(strikethrough) worker.append(TextFormat.STRIKETHROUGH);
        if(obfuscated) worker.append(TextFormat.OBFUSCATED);

        return worker;
    }

    @Override
    public String toString() {
        StringBuilder nukkitTextBuilder = addFormat(new StringBuilder());

        for(RawTextBuilder raw: extra) {
            nukkitTextBuilder = addFormat(nukkitTextBuilder);
            nukkitTextBuilder.append(raw.toString());
        }

        nukkitTextBuilder.append(TextFormat.RESET);

        return nukkitTextBuilder.toString();
    }
}
