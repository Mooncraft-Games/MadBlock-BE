package org.madblock.ranks.enums;

import cn.nukkit.utils.TextFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

@AllArgsConstructor @Getter
public enum PrimaryRankID {

    // NOTE: There cannot be more than 30 primary ranks without adjustments to the database format.
    PLAYER(generateId(0), "player", null, null),

    CONTENT(generateId(1), "content", "\uE116", TextFormat.BLUE), // Previously Artist
    CONTENT_2(generateId(2), "content2", "\uE115", TextFormat.BLUE), // Previously Builder

    HELPER(generateId(3), "helper", "\uE111", TextFormat.DARK_AQUA),
    MOD(generateId(4), "mod", "\uE110", TextFormat.DARK_AQUA),
    SRMOD(generateId(5), "sr_mod", "\uE112", TextFormat.DARK_AQUA),
    DEV(generateId(6), "dev", "\uE113", TextFormat.DARK_RED),
    ADMIN(generateId(7), "admin", "\uE114", TextFormat.DARK_RED),
    ADMIN_2(generateId(8), "admin2", "\uE114", TextFormat.DARK_RED), // Previously manager

    VIP(generateId(9), "vip", "\uE120", TextFormat.GREEN),
    VIP_PLUS(generateId(10), "vip_plus", "\uE121", TextFormat.AQUA),
    ELITE(generateId(11), "elite", "\uE122", TextFormat.LIGHT_PURPLE),
    OVERLORD(generateId(12), "overlord", "\uE123", TextFormat.DARK_GRAY),

    YT(generateId(13), "youtube", "\uE117", TextFormat.DARK_PURPLE);


    private final int id;
    private final String rankId;
    private final String name;
    private final TextFormat color;

    public Optional<String> getName() {
        return Optional.ofNullable(this.name);
    }

    public Optional<TextFormat> getColor() {
        return Optional.ofNullable(this.color);
    }

    private static int generateId(int rankIndex) {
        return (int)Math.pow(2, rankIndex);
    }
}