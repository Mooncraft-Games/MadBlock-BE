package org.madblock.playerregistry.link;

import org.madblock.playerregistry.PlayerRegistry;
import org.madblock.util.DatabaseResult;
import org.madblock.util.DatabaseReturn;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class ServiceLinker {

    // The following tests were all done using a 9 character source CHARACTERS array.

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
            'p', // pig
            'c', // cow
            'S', // squid
            'v', // villager
            's', // sheep

            'd', // diamond
            'E', // emerald
            'g', // gold ingot
            'i', // iron ingot
            'C', // coal
    };

    //TODO: Add to a config
    public static final long CODE_EXPIRE_LENGTH = 1000*60*20; // 20 minutes

    public static final int ERROR_CODE_DUPLICATE_KEY = 1062;

    public static final String SQL_CREATE_PENDING_LINK = "INSERT INTO pending_service_links (integration, identifier, code, expire) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE code=?, expire=?;";

    public static final String SQL_FETCH_LINK_DETAILS_FOR_CODE = "SELECT integration, identifier FROM pending_service_links WHERE code=? AND expire>?;";
    public static final String SQL_REMOVE_EXPIRED_CODES = "DELETE FROM pending_service_links WHERE expire<?;";

    public static final String SQL_FETCH_USER_INTEGRATION_WITH_XUID = "SELECT integration, identifier FROM integration_links WHERE xuid=? AND integration=?;";
    public static final String SQL_FETCH_USER_INTEGRATION_WITH_PLATFORM_ID = "SELECT xuid FROM integration_links WHERE identifier=? AND integration=?;";


    /**
     * Creates a code to be shown to the user in order to link
     * two services.
     *
     * @param service the service the link has been started from
     * @param id the identifier of the user on the source service
     *
     * @return the code to link with
     */
    public static DatabaseReturn<String> linkFromService(String service, String id) {

    }


    /**
     * Creates a code to be shown to the user in order to link
     * two services.
     *
     * @param service the service the link has been started from
     * @param id the identifier of the user on the source service
     *
     * @return the code to link with
     */
    public static DatabaseReturn<String> linkFromService(KnownLinkSources service, String id) {
        return ServiceLinker.linkFromService(service.getId(), id);
    }


    //TODO: Remember that the redeeming service is whatever the /link [code] has been ran on.
    //      and not where the initial /link was ran.
    public static DatabaseResult redeemLink(String code, String redeemingService) {

    }

    public static DatabaseResult redeemLink(String code, KnownLinkSources redeemingService) {
        return ServiceLinker.redeemLink(code, redeemingService.getId());
    }


    /**
     * Generates a new link code using the character set specified
     * by CHARACTERS.
     * @return a new link code
     */
    public static String generateCode() {
        SecureRandom random = new SecureRandom();

        // Firstly, draw a subset of characters from the larger
        // set to make memorisation easier.
        int size = Math.min(CODE_CHARSET_VARIETY, CHARACTERS.length); // ignore intellij's warnings, this is intended.
        char[] charSet = new char[size];
        ArrayList<Character> source = new ArrayList<>(Arrays.asList(CHARACTERS)); // must be Character and not char as java is dumb

        for(int i = 0; i < charSet.length; i++) {
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
        int longestDupeStreak = 0;


        for(int run = 0; run < runs; run++) {
            HashSet<String> pastCodes = new HashSet<>();

            int dupes = 0;
            int firstDupe = -1;
            int dupeStreak = 0;

            for (int i = 0; i < codesPerRun; i++) {
                String code = IntegrationLinker.generateCode();
                //System.out.printf("Code: %s\n", code);

                if (pastCodes.contains(code)) {
                    dupes++;
                    dupeStreak += 1;
                    if (firstDupe < 0) firstDupe = i;

                } else {
                    pastCodes.add(code);
                    if(dupeStreak > longestDupeStreak)
                        longestDupeStreak = dupeStreak;
                    dupeStreak = 0;
                }
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
        PlayerRegistry.get().getLogger().info(String.format("Longest Dupe Streak: %s%n", longestDupeStreak));
        PlayerRegistry.get().getLogger().info(String.format("Avg Dupes: %s%n", avgDupes));
        PlayerRegistry.get().getLogger().info(String.format("Avg First Dupe Index: %s%n", avgFirstDupe));

        return new Number[] { totalDupes, longestDupeStreak, avgDupes, avgFirstDupe };
    }
}
