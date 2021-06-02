package org.madblock.ranks.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public enum SubRankID {
    COMMUNITY_RELATIONS(generateId(1), "com_rel"), // Reports, feedback, events
    MEDIA(generateId(2), "social_media"), // YT Management, Social Media
    STAFF_COORDINATION(generateId(3), "staff_coord"),
    SUPPORT(generateId(4), "support"), // Reports, Appeals
    TOURNEY(generateId(5), "tourney");

    private final int id;
    private final String groupId;

    private static int generateId(int rankIndex) {
        return (int)Math.pow(2, rankIndex);
    }
}