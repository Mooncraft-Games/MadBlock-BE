package org.madblock.playerregistry.link;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import org.madblock.database.ConnectionWrapper;
import org.madblock.database.DatabaseAPI;
import org.madblock.database.DatabaseStatement;
import org.madblock.database.DatabaseUtility;
import org.madblock.lib.commons.style.Check;
import org.madblock.playerregistry.PlayerRegistry;
import org.madblock.playerregistry.PlayerRegistryReturns;
import org.madblock.util.DatabaseResult;
import org.madblock.util.DatabaseReturn;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class IntegrationLinker {

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

    //TODO: Add to a config
    public static final long CODE_EXPIRE_LENGTH = 1000*60*20; // 20 minutes
    public static final int MAX_CODE_GEN_ATTEMPTS = 5;

    public static final int ERROR_CODE_DUPLICATE_KEY = 1062;

    public static final String SQL_CREATE_PENDING_LINK = "INSERT INTO pending_service_links (integration, identifier, code, expire) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE code=?, expire=?;";
    public static final String SQL_CREATE_ESTABLISHED_LINK = "INSERT INTO integration_links (xuid, integration, identifier) VALUES (?, ?, ?);";

    public static final String SQL_FETCH_LINK_DETAILS_FOR_CODE = "SELECT integration, identifier FROM pending_service_links WHERE code=? AND expire>?;";
    public static final String SQL_REMOVE_EXPIRED_CODES = "DELETE FROM pending_service_links WHERE expire<?;";
    public static final String SQL_REMOVE_COMPLETED_CODE = "DELETE FROM pending_service_links WHERE code=?;";

    public static final String SQL_FETCH_USER_INTEGRATION_WITH_XUID = "SELECT integration, identifier FROM integration_links WHERE xuid=? AND integration=?;";
    public static final String SQL_FETCH_USER_INTEGRATION_WITH_PLATFORM_ID = "SELECT xuid FROM integration_links WHERE identifier=? AND integration=?;";



    /**
     * Creates a code to be shown to the user in order to link
     * two platforms.
     *
     * @param platform the platform the link has been started from
     * @param id the identifier of the user on the source platform
     *
     * @return the code to link with
     */
    public static DatabaseReturn<String> linkFromPlatform(String platform, String id) {
        Check.notEmptyString(platform, "platform");
        Check.notEmptyString(id, "id");

        ConnectionWrapper wrapper;

        try {
            wrapper = DatabaseAPI.getConnection("MAIN");
        } catch (SQLException err) {
            err.printStackTrace();
            return DatabaseReturn.empty(DatabaseResult.DATABASE_OFFLINE);
        } catch (Exception err) {
            err.printStackTrace();
            return DatabaseReturn.empty(DatabaseResult.ERROR, PlayerRegistryReturns.UNKNOWN_CONNECTION_ERROR);
        }

        // Enter a transaction as we're chaining multiple statements.
        try {
            wrapper.getConnection().setAutoCommit(false);
        } catch (Exception err) {
            err.printStackTrace();
            DatabaseUtility.closeQuietly(wrapper);
            return DatabaseReturn.empty(DatabaseResult.ERROR, PlayerRegistryReturns.FAILED_TO_OBTAIN_LOCK);
        }


        // As the goal is to link back to minecraft from other platforms, you can check
        // if they have a link already. The other way around does not have the shortcut below.
        if(!platform.equals(KnownLinkSources.MINECRAFT.getId())) {
            PreparedStatement stmtCheckIntegrations = null;
            ResultSet resultSet;
            try {
                stmtCheckIntegrations = wrapper.prepareStatement(new DatabaseStatement(SQL_FETCH_USER_INTEGRATION_WITH_PLATFORM_ID, new Object[] {platform, id} ));
                resultSet = stmtCheckIntegrations.executeQuery();

                // Integration already exists, abort.
                if(resultSet.next()) {
                    IntegrationLinker.rollbackQuietly(wrapper.getConnection());
                    DatabaseUtility.closeQuietly(stmtCheckIntegrations);
                    DatabaseUtility.closeQuietly(wrapper);
                    return DatabaseReturn.empty(DatabaseResult.FAILURE, PlayerRegistryReturns.INTEGRATION_ALREADY_EXISTS);
                }

            } catch (Exception err) {
                IntegrationLinker.rollbackQuietly(wrapper.getConnection());
                DatabaseUtility.closeQuietly(stmtCheckIntegrations);
                DatabaseUtility.closeQuietly(wrapper);
                err.printStackTrace();
                return DatabaseReturn.empty(DatabaseResult.ERROR, PlayerRegistryReturns.INTEGRATION_EXISTENCE_CHECK_ERRORED);
            }

            DatabaseUtility.closeQuietly(stmtCheckIntegrations);
        }


        String code = null;

        // From testing with 100000 codes, averaging out multiple runs, the most
        // back to back collisions was 3. - 5 should be enough.
        for(int attempt = 0; attempt < MAX_CODE_GEN_ATTEMPTS; attempt++) {
            code = IntegrationLinker.generateCode();
            long expiryTime = System.currentTimeMillis() + CODE_EXPIRE_LENGTH;

            PreparedStatement stmtPublishCode = null;
            try {
                stmtPublishCode = wrapper.prepareStatement(new DatabaseStatement(SQL_CREATE_PENDING_LINK, new Object[] { platform, id, code, expiryTime, code, expiryTime } ));
                stmtPublishCode.executeUpdate();

            } catch (SQLException err) {
                DatabaseUtility.closeQuietly(stmtPublishCode);

                int errCode = err.getErrorCode();
                if(errCode == ERROR_CODE_DUPLICATE_KEY) {
                    code = null;
                    continue; // duplicate key, try again
                }

                IntegrationLinker.rollbackQuietly(wrapper.getConnection());
                DatabaseUtility.closeQuietly(wrapper);
                err.printStackTrace();
                return DatabaseReturn.empty(DatabaseResult.ERROR, PlayerRegistryReturns.INTEGRATION_CODE_GENERATION_ERRORED);

            } catch (Exception err) {
                IntegrationLinker.rollbackQuietly(wrapper.getConnection());
                DatabaseUtility.closeQuietly(stmtPublishCode);
                DatabaseUtility.closeQuietly(wrapper);

                err.printStackTrace();
                return DatabaseReturn.empty(DatabaseResult.ERROR, PlayerRegistryReturns.INTEGRATION_CODE_GENERATION_ERRORED);
            }

            DatabaseUtility.closeQuietly(stmtPublishCode);
            break;
        }



        try {
            wrapper.getConnection().commit();
        } catch (Exception err) {
            IntegrationLinker.rollbackQuietly(wrapper.getConnection());
            DatabaseUtility.closeQuietly(wrapper);

            err.printStackTrace();
            return DatabaseReturn.empty(DatabaseResult.ERROR, PlayerRegistryReturns.FAILED_TO_RELEASE_LOCK);
        }


        DatabaseUtility.closeQuietly(wrapper);
        return code != null
                ? DatabaseReturn.of(code, DatabaseResult.SUCCESS)
                : DatabaseReturn.empty(DatabaseResult.FAILURE, PlayerRegistryReturns.INTEGRATION_ALL_LINK_CODES_DUPES);
    }


    /**
     * Creates a code to be shown to the user in order to link
     * two platforms.
     *
     * @param service the platform the link has been started from
     * @param id the identifier of the user on the source platform
     *
     * @return the code to link with
     */
    public static DatabaseReturn<String> linkFromPlatform(KnownLinkSources service, String id) {
        return IntegrationLinker.linkFromPlatform(service.getId(), id);
    }



    // TODO: Return the name of the service that generated the code
    public static DatabaseReturn<String> redeemLink(String code, String redeemingPlatform, String redeemingIdentifier) {
        Check.notEmptyString(code, "code");
        Check.notEmptyString(redeemingPlatform, "redeemingPlatform");
        Check.notEmptyString(redeemingIdentifier, "redeemingIdentifier");

        ConnectionWrapper wrapper;

        try {
            wrapper = DatabaseAPI.getConnection("MAIN");

        } catch (SQLException err) {
            err.printStackTrace();
            return DatabaseReturn.empty(DatabaseResult.DATABASE_OFFLINE);
        } catch (Exception err) {
            err.printStackTrace();
            return DatabaseReturn.empty(DatabaseResult.ERROR, PlayerRegistryReturns.UNKNOWN_CONNECTION_ERROR);
        }

        try {
            wrapper.getConnection().setAutoCommit(false);
        } catch (Exception err) {
            err.printStackTrace();
            DatabaseUtility.closeQuietly(wrapper);
            return DatabaseReturn.empty(DatabaseResult.ERROR, PlayerRegistryReturns.FAILED_TO_OBTAIN_LOCK);
        }

        // Attempt to check the code against the pending database to get the
        PreparedStatement stmtGetLinkForCode = null;
        String linkingPlatform = null;
        String linkingIdentifier = null;
        try {
            // code + current timestamp
            stmtGetLinkForCode = wrapper.prepareStatement(new DatabaseStatement(SQL_FETCH_LINK_DETAILS_FOR_CODE, new Object[]{ code, System.currentTimeMillis() }) );
            ResultSet results = stmtGetLinkForCode.executeQuery();

            if(results.next()) {
                linkingPlatform = results.getString("integration");
                linkingIdentifier = results.getString("identifier");

                if(linkingPlatform.equals(redeemingPlatform)) {
                    IntegrationLinker.rollbackQuietly(wrapper.getConnection());
                    DatabaseUtility.closeQuietly(stmtGetLinkForCode);
                    DatabaseUtility.closeQuietly(wrapper);
                    return DatabaseReturn.empty(DatabaseResult.ERROR, PlayerRegistryReturns.LINK_SAME_PLATFORM);
                }

            }

        } catch (Exception err) {
            IntegrationLinker.rollbackQuietly(wrapper.getConnection());
            DatabaseUtility.closeQuietly(stmtGetLinkForCode);
            DatabaseUtility.closeQuietly(wrapper);

            err.printStackTrace();
            return DatabaseReturn.empty(DatabaseResult.ERROR, PlayerRegistryReturns.LINK_FETCH_DETAILS_FROM_CODE_ERRORED);
        }

        DatabaseUtility.closeQuietly(stmtGetLinkForCode);

        if(Check.isStringEmpty(linkingPlatform) || Check.isStringEmpty(linkingIdentifier)) {
            IntegrationLinker.rollbackQuietly(wrapper.getConnection());
            DatabaseUtility.closeQuietly(wrapper);

            return DatabaseReturn.empty(DatabaseResult.FAILURE, PlayerRegistryReturns.LINK_NONE_FOUND);
        }

        boolean redeemerIsMinecraft = false;
        String xuid, integration, integrationIdentifier;

        // Get whichever platform is minecraft, assign the xuid based off that, and
        // sort the order out.
        // If neither, someone messed up.
        if(redeemingPlatform.equals(KnownLinkSources.MINECRAFT.getId())) {
            redeemerIsMinecraft = true;
            xuid = redeemingIdentifier;
            integration = linkingPlatform;
            integrationIdentifier = linkingIdentifier;

        } else if(linkingPlatform.equals(KnownLinkSources.MINECRAFT.getId())) {
            redeemerIsMinecraft = false;
            xuid = linkingIdentifier;
            integration = redeemingPlatform;
            integrationIdentifier = redeemingIdentifier;

        } else {
            IntegrationLinker.rollbackQuietly(wrapper.getConnection());
            DatabaseUtility.closeQuietly(wrapper);

            return DatabaseReturn.empty(DatabaseResult.FAILURE, PlayerRegistryReturns.LINK_INCOMPATIBLE_PLATFORM);
        }


        // Code is valid, attempt to
        PreparedStatement stmtPublishLink = null;
        PreparedStatement stmtDeleteLinkCode = null;
        try {

            stmtPublishLink = wrapper.prepareStatement(new DatabaseStatement(SQL_CREATE_ESTABLISHED_LINK, new Object[]{ xuid, integration, integrationIdentifier }));
            stmtPublishLink.executeUpdate();

            stmtDeleteLinkCode = wrapper.prepareStatement(new DatabaseStatement(SQL_REMOVE_COMPLETED_CODE, new Object[]{ code }));
            stmtDeleteLinkCode.executeUpdate();

        } catch (Exception err) {
            // rollback changes if there's a clash
            IntegrationLinker.rollbackQuietly(wrapper.getConnection());
            DatabaseUtility.closeQuietly(stmtPublishLink);
            DatabaseUtility.closeQuietly(stmtDeleteLinkCode);
            DatabaseUtility.closeQuietly(wrapper);

            return DatabaseReturn.empty(DatabaseResult.ERROR, PlayerRegistryReturns.LINK_CREATE_PAIRING_ERRORED);
        }

        DatabaseUtility.closeQuietly(stmtPublishLink);
        DatabaseUtility.closeQuietly(stmtDeleteLinkCode);

        // commit all changes made previously.
        try {
            wrapper.getConnection().commit();

        } catch (Exception err) {
            IntegrationLinker.rollbackQuietly(wrapper.getConnection());
            DatabaseUtility.closeQuietly(wrapper);

            err.printStackTrace();
            return DatabaseReturn.empty(DatabaseResult.ERROR, PlayerRegistryReturns.FAILED_TO_RELEASE_LOCK);
        }

        DatabaseUtility.closeQuietly(wrapper);
        return redeemerIsMinecraft
                ? DatabaseReturn.of(linkingPlatform, DatabaseResult.SUCCESS)
                : DatabaseReturn.of(redeemingPlatform, DatabaseResult.SUCCESS);
    }

    public static DatabaseReturn<String> redeemLink(String code, KnownLinkSources redeemingPlatform, String redeemingIdentifier) {
        return IntegrationLinker.redeemLink(code, redeemingPlatform.getId(), redeemingIdentifier);
    }


    private static void rollbackQuietly(Connection connection) {
        try { connection.rollback(); }
        catch (Exception ignored) { }
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
        int size = Math.min(CODE_CHARSET_VARIETY, CodeGeneratorSymbols.CHARACTERS.length); // ignore intellij's warnings, this is intended.
        char[] charSet = new char[size];
        ArrayList<Character> source = new ArrayList<>(Arrays.asList(CodeGeneratorSymbols.CHARACTERS)); // must be Character and not char as java is dumb

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
                    if (dupeStreak > longestDupeStreak)
                        longestDupeStreak = dupeStreak;
                    dupeStreak = 0;
                }
            }
        }


        double avgFirstDupe = (double) totalFirstDupe / runs;
        double avgDupes = (double) totalDupes / runs;

        PlayerRegistry.get().getLogger().info("- Settings ----");
        PlayerRegistry.get().getLogger().info(String.format("CODE_CHARSET_VARIETY: %s%n", IntegrationLinker.CODE_CHARSET_VARIETY));
        PlayerRegistry.get().getLogger().info(String.format("CODE_VARIETY: %s%n", IntegrationLinker.CODE_VARIETY));
        PlayerRegistry.get().getLogger().info(String.format("CODE_BLOCK_SIZE: %s%n", IntegrationLinker.CODE_BLOCK_SIZE));

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
