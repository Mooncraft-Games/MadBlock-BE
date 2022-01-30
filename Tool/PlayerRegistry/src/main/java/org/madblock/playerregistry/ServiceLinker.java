package org.madblock.playerregistry;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class ServiceLinker {

    /*
     * - Example ----
     * abc-def
     *
     * - Settings ----
     * CODE_CHARSET_VARIETY: 6
     * CODE_VARIETY: 6
     * CODE_BLOCK_SIZE: 3
     *
     * - Stats ----
     * Runs: 100
     * Codes Per Run: 10000
     *
     * Total Dupes: 14230.0
     * Avg Dupes: 142.3
     * Avg First Dupe Index: 702.28
     */

    /*
     *- Example ----
     * abc-def-abc
     *
     *- Settings ----
     * CODE_CHARSET_VARIETY: 6
     * CODE_VARIETY: 8
     * CODE_BLOCK_SIZE: 4
     *
     * - Stats ----
     * Runs: 100
     * Codes Per Run: 10000
     *
     * Total Dupes: 245.0
     * Avg Dupes: 2.45
     * Avg First Dupe Index: 4793.23
     */

    /*
     *- Example ----
     * abcd-efab (CURRENT SETTINGS)
     *
     *- Settings ----
     * CODE_CHARSET_VARIETY: 6
     * CODE_VARIETY: 8
     * CODE_BLOCK_SIZE: 4
     *
     * - Stats ----
     * Runs: 100
     * Codes Per Run: 10000
     *
     * Total Dupes: 245.0
     * Avg Dupes: 2.45
     * Avg First Dupe Index: 4793.23
     */
    public static final int CODE_CHARSET_VARIETY = 6;
    public static final int CODE_VARIETY = 8;
    public static final int CODE_BLOCK_SIZE = 4;

    public static final Character[] CHARACTERS = new Character[] {
            'c', // creeper
            'z', // zombie
            'e', // enderman

            'v', // villager
            'p', // pig
            's', // sheep

            'd', // diamond
            'g', // gold ingot
            'i'  // iron ingot
    };




    /**
     * Generates a new link code using the character set specified
     * by CHARACTERS.
     * @return a new link code
     */
    public static String generateCode() {
        SecureRandom random = new SecureRandom();

        // Firstly, draw a subset of characters from the larger
        // set to make memorisation easier.
        char[] charSet = new char[CODE_CHARSET_VARIETY];
        ArrayList<Character> source = new ArrayList<>(Arrays.asList(CHARACTERS)); // must be Character and not char as java is dumb

        for(int i = 0; i < CODE_CHARSET_VARIETY; i++) {
            int index = random.nextInt(source.size());
            Character chr = source.remove(index);

            charSet[i] = chr;
        }



        // Secondly, draw from this new character set
        // and build a code, splitting every CODE_BLOCK_SIZE characters.
        StringBuilder code = new StringBuilder();

        for(int i = 0; i < CODE_VARIETY; i++) {
            if((Math.floorMod(i, CODE_BLOCK_SIZE) == 0) && (i > 0))
                code.append("-");

            int chr = random.nextInt(charSet.length);
            code.append(charSet[chr]);
        }

        return code.toString();
    }


    // This could maybe be moved to a unit test with quotas
    // for collisions? idk
    private static Number[] testCodeGeneration(int runs, int codesPerRun) {
        float totalFirstDupe = 0;
        float totalDupes = 0;


        for(int run = 0; run < runs; run++) {
            HashSet<String> pastCodes = new HashSet<>();

            int dupes = 0;
            int firstDupe = -1;

            for (int i = 0; i < codesPerRun; i++) {
                String code = ServiceLinker.generateCode();
                //System.out.printf("Code: %s\n", code);

                if (pastCodes.contains(code)) {
                    dupes++;
                    if (firstDupe < 0) firstDupe = i;

                } else pastCodes.add(code);
            }

            totalDupes += dupes;
            totalFirstDupe += firstDupe;
        }

        double avgFirstDupe = (double) totalFirstDupe / runs;
        double avgDupes = (double) totalDupes / runs;

        PlayerRegistry.get().getLogger().info("- Settings ----");
        PlayerRegistry.get().getLogger().info(String.format("CODE_CHARSET_VARIETY: %s%n", ServiceLinker.CODE_CHARSET_VARIETY));
        PlayerRegistry.get().getLogger().info(String.format("CODE_VARIETY: %s%n", ServiceLinker.CODE_VARIETY));
        PlayerRegistry.get().getLogger().info(String.format("CODE_BLOCK_SIZE: %s%n", ServiceLinker.CODE_BLOCK_SIZE));

        PlayerRegistry.get().getLogger().info(String.format("%n- Stats ----%n"));

        PlayerRegistry.get().getLogger().info(String.format("Runs: %s%n", runs));
        PlayerRegistry.get().getLogger().info(String.format("Codes Per Run: %s%n", codesPerRun));

        PlayerRegistry.get().getLogger().info(String.format("%nTotal Dupes: %s%n", totalDupes));
        PlayerRegistry.get().getLogger().info(String.format("Avg Dupes: %s%n", avgDupes));
        PlayerRegistry.get().getLogger().info(String.format("Avg First Dupe Index: %s%n", avgFirstDupe));

        return new Number[] { totalDupes, avgDupes, avgFirstDupe };
    }
}
