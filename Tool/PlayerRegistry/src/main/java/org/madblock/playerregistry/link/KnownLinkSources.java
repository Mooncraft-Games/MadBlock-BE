package org.madblock.playerregistry.link;

public enum KnownLinkSources {

    MINECRAFT("Minecraft"), // servers all use xuid, group under one.
    DISCORD("Discord"),

    UNKNOWN("???"); // what.

    private final String id;

    KnownLinkSources(String id) {
        this.id = id;
    }

    public final String getId() {
        return id;
    }


    public static KnownLinkSources fromID(String id) {
        if(id == null) return UNKNOWN;

        switch (id.trim().toLowerCase()) {

            case "minecraft":
                return MINECRAFT;

            case "discord":
                return DISCORD;

            default:
                return UNKNOWN;
        }
    }
}
