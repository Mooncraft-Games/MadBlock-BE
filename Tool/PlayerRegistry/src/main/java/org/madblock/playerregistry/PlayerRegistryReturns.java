package org.madblock.playerregistry;

public class PlayerRegistryReturns {

    public static final String UNKNOWN_CONNECTION_ERROR = "unexpected_error_during_connection";
    public static final String FAILED_TO_OBTAIN_LOCK = "transaction_creation_failure";
    public static final String FAILED_TO_RELEASE_LOCK = "transaction_completion_failure";


    public static final String INTEGRATION_EXISTENCE_CHECK_ERRORED = "existing_integration_check_error";
    public static final String INTEGRATION_CODE_GENERATION_ERRORED = "integration_code_gen_error";

    public static final String INTEGRATION_ALREADY_EXISTS = "existing_integration_present";
    public static final String INTEGRATION_ALL_LINK_CODES_DUPES = "all_link_codes_duplicates";

}
