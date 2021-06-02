package org.madblock.ranks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PermissionGroup {

    // -- BASE RANKS --

    public static final List<String> EMPTY = Collections.unmodifiableList(new ArrayList<>());
    public static final List<String> BASE = extendPermissions(EMPTY,
            "pocketvote.vote", "is.command.expel", "is.command.leave", "is.command.lang", "is.command.protection",
            "is.command.setting", "is.topten", "is.command.download", "is.create", "is.command.reset", "is.command.home",
            "is.command.teleport", "is.command.teamChat", "is.command.messages", "is.command.accept", "is.command.reject",
            "is.command.invite", "is.command.quit");

    // -- PREMIUM RANKS --

    // -- STAFF RANK GROUPS --

    public static final List<String> BASE_STAFF = extendPermissions(BASE, "lobby.build_area.build");
    public static final List<String> MINI_MOD = extendPermissions(BASE_STAFF, "punishments.issue.limited");
    public static final List<String> MODERATOR = extendAndOverridePermissions(BASE_STAFF, new String[]{"punishments.issue.limited"}, "punishments.issue.full", "newgameapi.tourney");

    public static final List<String> CONTENT = extendPermissions(BASE_STAFF, "newgameapi.commands.game");

    public static final List<String> ADMINISTRATOR = extendPermissions(MODERATOR, "punishments.issue.ntb", "newgameapi.commands.game", "ranks.commands.rank", "newgameapi.commands.tourney", "newgameapi.commands.super", "newgameapi.commands.addstat", "punishments.reports.manage", "is.admin.command", "is.admin.generate", "is.admin.clear", "is.admin.kick", "is.admin.rename", "is.admin.cobblestats", "is.admin.delete");

    // -- SUB-RANKS --

    public static final List<String> COMMUNITY_RELATIONS = extendAndOverridePermissions(EMPTY, new String[]{"punishments.issue.limited"}, "punishments.issue.full", "punishments.reports.manage");

    public static final List<String> TOURNEY = extendPermissions(EMPTY, "newgameapi.tourney", "newgameapi.commands.tourney");
    public static final List<String> SUPPORT = extendAndOverridePermissions(EMPTY, new String[]{"punishments.issue.limited"}, "punishments.issue.full", "punishments.reports.manage");
    public static final List<String> MEDIA = extendPermissions(TOURNEY, "newgameapi.commands.super");



    // Methods :)

    public static List<String> extendPermissions(List<String> base, String... permissions){
        return extendAndOverridePermissions(base, new String[0], permissions);
    }

    public static List<String> extendAndOverridePermissions(List<String> base, String[] removals, String... additions){
        ArrayList<String> newList = new ArrayList<>(base);
        newList.removeAll(Arrays.asList(removals));
        newList.addAll(Arrays.asList(additions));

        return Collections.unmodifiableList(newList);
    }

}