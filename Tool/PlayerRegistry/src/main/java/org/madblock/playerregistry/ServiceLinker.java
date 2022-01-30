package org.madblock.playerregistry;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServiceLinker {

    public static final int CODE_CHARSET_VARIETY = 6;
    public static final int CODE_VARIETY = 6;
    public static final int CODE_BLOCK_SIZE = 3;

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
    private static String generateCode() {
        SecureRandom random = new SecureRandom();

        // Firstly, draw a subset of characters from the larger
        // set to make memorisation easier.
        char[] charSet = new char[CODE_VARIETY];
        List<Character> source = Arrays.asList(CHARACTERS); // must be Character and not char as java is dumb

        for(int i = 0; i < CODE_VARIETY; i++) {
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
            code.append(chr);
        }

        return code.toString();
    }
}
