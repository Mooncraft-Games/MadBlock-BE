package org.madblock.playerregistry;

import java.security.SecureRandom;

public class ServiceLinker {

    public static final String[] CHARACTERS = new String[] {
            "c", // creeper
            "z", // zombie
            "e", // enderman

            "v", // villager
            "p", // pig
            "s", // sheep

            "d", // diamond
            "g", // gold ingot
            "i"  // iron ingot
    };


    /**
     * Generates a new link code using the character set specified
     * by CHARACTERS.
     * @return a new link code
     */
    private static String generateCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();

        for(int i = 0; i < 6; i++) {
            int chr = random.nextInt(CHARACTERS.length);
            code.append(chr);
        }

        code.insert(3, "-");
        return code.toString();
    }
}
