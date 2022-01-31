package org.madblock.playerregistry.link;

import org.madblock.lib.commons.style.Check;
import java.util.ArrayList;

public final class CodeGeneratorSymbols {


    public static final Character[] CHARACTERS;

    private static final ArrayList<Character> charactersInternal = new ArrayList<>();
    private static final ArrayList<String> graphicsInternal = new ArrayList<>();
    private static final ArrayList<String> namesInternal = new ArrayList<>();

    private static void registerCodeSymbol(Character symbol, String graphic, String name) {
        CodeGeneratorSymbols.charactersInternal.add(symbol);
        CodeGeneratorSymbols.graphicsInternal.add(graphic);
        CodeGeneratorSymbols.namesInternal.add(name);
    }

    static {
        CodeGeneratorSymbols.registerCodeSymbol('p', "\uE200", "Pig");
        CodeGeneratorSymbols.registerCodeSymbol('c', "\uE201", "Cow");
        CodeGeneratorSymbols.registerCodeSymbol('S', "\uE202", "Squid");
        CodeGeneratorSymbols.registerCodeSymbol('v', "\uE203", "Villager");
        CodeGeneratorSymbols.registerCodeSymbol('s', "\uE204", "Sheep");

        CodeGeneratorSymbols.registerCodeSymbol('d', "\uE205", "Diamond");
        CodeGeneratorSymbols.registerCodeSymbol('e', "\uE206", "Emerald");
        CodeGeneratorSymbols.registerCodeSymbol('g', "\uE207", "Gold");
        CodeGeneratorSymbols.registerCodeSymbol('i', "\uE208", "Iron");
        CodeGeneratorSymbols.registerCodeSymbol('C', "\uE209", "Coal");

        CHARACTERS = charactersInternal.toArray(new Character[0]);
    }



    public static String getGraphicFromCode(Character code) {
        Check.nullParam(code, "code");
        int index = CodeGeneratorSymbols.charactersInternal.indexOf(code);

        return index > -1
                ? CodeGeneratorSymbols.graphicsInternal.get(index)
                : "";
    }

    public static String getNameFromCode(Character code) {
        Check.nullParam(code, "code");
        int index = CodeGeneratorSymbols.charactersInternal.indexOf(code);

        return index > -1
                ? CodeGeneratorSymbols.namesInternal.get(index)
                : "";
    }

    public static Character getCodeFromGraphic(String graphic) {
        Check.nullParam(graphic, "graphic");
        int index = CodeGeneratorSymbols.graphicsInternal.indexOf(graphic);

        return index > -1
                ? CodeGeneratorSymbols.charactersInternal.get(index)
                : ' ';
    }

    public static Character getCodeFromName(String name) {
        Check.nullParam(name, "name");
        int index = CodeGeneratorSymbols.namesInternal.indexOf(name);

        return index > -1
                ? CodeGeneratorSymbols.charactersInternal.get(index)
                : ' ';
    }



    public static ArrayList<String> getGraphics() {
        return new ArrayList<>(graphicsInternal);
    }

    public static ArrayList<String> getNamesInternal() {
        return new ArrayList<>(namesInternal);
    }
}
