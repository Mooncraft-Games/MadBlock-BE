package org.madblock.playerregistry;

public class PlayerServerLocation {
    private final String ip;
    private final int port;

    public PlayerServerLocation(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}