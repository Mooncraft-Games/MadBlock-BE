package org.madblock.newgamesapi.map.functionalregions;

import cn.nukkit.level.Level;
import org.madblock.newgamesapi.map.types.MapRegion;

import java.util.Optional;
import java.util.regex.Pattern;

public class FunctionalRegionCallData {

    private MapRegion region;
    private Level level;
    private String tag;
    private String[] args;

    public FunctionalRegionCallData(MapRegion region, Level level, String tag, String[] args){
        this.region = region;
        this.level = level;
        this.tag = tag;
        this.args = args;
    }

    public MapRegion getRegion() {
        return region;
    }

    public Level getLevel() {
        return level;
    }

    public String getTag() {
        return tag;
    }

    public String[] getArgs() {
        return args;
    }

    public static ParseResult parseTagArgs(String fulltag){
        char[] chars = fulltag.toCharArray();
        String tagString = "";
        String argString = "";
        boolean parsingArgs = false;
        for(int i = 0; i < chars.length; i++){
            String character = String.valueOf(chars[i]);
            if(parsingArgs){
                if(character.equals(")")){
                    break;
                } else {
                    argString = argString.concat(character);
                }
            } else {
                if(character.equals("(")){
                    parsingArgs = true;
                } else {
                    tagString = tagString.concat(character);
                }
            }
        }
        String[] argumentsSplit = argString.split(Pattern.quote(","));
        for(int i = 0; i < argumentsSplit.length; i++){
            String a = argumentsSplit[i];
            argumentsSplit[i] = a.trim();
        }
        return new ParseResult(tagString, argumentsSplit);
    }

    public static Optional<Boolean> parseBoolean(String booleanval){
        if(booleanval != null){
            return Optional.of(Boolean.parseBoolean(booleanval));
        } else {
            return Optional.empty();
        }
    }

    public static Optional<Integer> parseInteger(String integerval){
        try {
            return Optional.of(Integer.parseInt(integerval));
        } catch (NumberFormatException err){
            return Optional.empty();
        }
    }

    public static Optional<Float> parseFloat(String floatval){
        try {
            return Optional.of(Float.parseFloat(floatval));
        } catch (NumberFormatException err){
            return Optional.empty();
        }
    }

    public static Optional<Double> parseDouble(String doubleval){
        try {
            return Optional.of(Double.parseDouble(doubleval));
        } catch (NumberFormatException err){
            return Optional.empty();
        }
    }

    public static final class ParseResult{
        private String tag;
        private String[] args;
        protected ParseResult(String tag, String[] args){
            this.tag = tag;
            this.args = args;
        }

        public String getTag() { return tag; }
        public String[] getArgs() { return args; }
    }

}
