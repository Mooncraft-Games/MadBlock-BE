package org.madblock.newgamesapi.exception;

public class PotentialMaxGamesReachedException extends RuntimeException {

    public PotentialMaxGamesReachedException(String message) {
        super(message);
    }

}
