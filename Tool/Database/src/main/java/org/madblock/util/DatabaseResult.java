package org.madblock.util;

public enum DatabaseResult {

    SUCCESS(true),
    SUCCESS_WITH_NO_CHANGE(true), // update/insert, if not change is made but an intended action is done.

    FAILURE(false),
    DATABASE_OFFLINE(false),
    ERROR(false);


    private final boolean isSuccess;

    DatabaseResult(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public boolean isSuccess() {
        return isSuccess;
    }
}
