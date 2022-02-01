package org.madblock.playerregistry;

public class PlayerRegistryReturns {

    public static final String UNKNOWN_CONNECTION_ERROR = "unexpected_error_during_connection"; // D0
    public static final String FAILED_TO_OBTAIN_LOCK = "transaction_creation_failure"; // DT0
    public static final String FAILED_TO_RELEASE_LOCK = "transaction_completion_failure"; // DT1


    public static final String INTEGRATION_EXISTENCE_CHECK_ERRORED = "existing_integration_check_error"; // G1
    public static final String INTEGRATION_CODE_GENERATION_ERRORED = "integration_code_gen_error"; // P1
    public static final String INTEGRATION_ALREADY_EXISTS = "existing_integration_present";
    public static final String INTEGRATION_ALL_LINK_CODES_DUPES = "all_link_codes_duplicates";

    public static final String LINK_FETCH_DETAILS_FROM_CODE_ERRORED = "fetch_link_details_error"; // G0
    public static final String LINK_CREATE_PAIRING_ERRORED = "create_link_pairing_error"; // P0
    public static final String LINK_NONE_FOUND = "no_pending_link_found";
    public static final String LINK_SAME_PLATFORM = "redeemed_on_same_platform";
    public static final String LINK_INCOMPATIBLE_PLATFORM = "redeemed_on_incompatible_platform";


    // E0/F0 if it's none of the above.
}
