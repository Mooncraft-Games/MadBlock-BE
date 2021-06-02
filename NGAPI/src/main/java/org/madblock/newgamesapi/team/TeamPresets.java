package org.madblock.newgamesapi.team;

public class TeamPresets {

    public static final String SPECTATOR_TEAM_ID = "spectators";
    public static final String DEAD_TEAM_ID = "dead";

    /** For internal use. */
    public static final Team.GenericTeamBuilder[] DEFAULT = new Team.GenericTeamBuilder[]{
            SpectatingTeam.newSpectatorTeamBuilder(SPECTATOR_TEAM_ID, "SPECTATOR", Team.Colour.LIGHT_GRAY),
            DeadTeam.newDeadTeamBuilder(DEAD_TEAM_ID, "DEAD", Team.Colour.GRAY)
    };

    public static final Team.GenericTeamBuilder[] FREE_FOR_ALL = new Team.GenericTeamBuilder[]{
            Team.newBasicTeamBuilder("players", "Players", Team.Colour.BLUE).setFriendlyFireEnabled(true).setCanPlayersDropItems(false).setCanPlayersPickUpItems(false)
    };

    public static final Team.GenericTeamBuilder[] TWO_TEAMS = new Team.GenericTeamBuilder[]{
            Team.newBasicTeamBuilder("red", "Red", Team.Colour.RED),
            Team.newBasicTeamBuilder("blue", "Blue", Team.Colour.BLUE)
    };

    public static final Team.GenericTeamBuilder[] FOUR_TEAMS = new Team.GenericTeamBuilder[]{
            Team.newBasicTeamBuilder("red", "Red", Team.Colour.RED),
            Team.newBasicTeamBuilder("yellow", "Yellow", Team.Colour.YELLOW),
            Team.newBasicTeamBuilder("green", "Green", Team.Colour.LIME),
            Team.newBasicTeamBuilder("blue", "Blue", Team.Colour.BLUE)
    };

    public static final Team.GenericTeamBuilder[] EIGHT_TEAMS = new Team.GenericTeamBuilder[]{
            Team.newBasicTeamBuilder("red", "Red", Team.Colour.RED),
            Team.newBasicTeamBuilder("orange", "Orange", Team.Colour.ORANGE),
            Team.newBasicTeamBuilder("yellow", "Yellow", Team.Colour.YELLOW),
            Team.newBasicTeamBuilder("green", "Green", Team.Colour.LIME),
            Team.newBasicTeamBuilder("blue", "Blue", Team.Colour.BLUE),
            Team.newBasicTeamBuilder("indigo", "Indigo", Team.Colour.INDIGO),
            Team.newBasicTeamBuilder("purple", "Purple", Team.Colour.PURPLE),
            Team.newBasicTeamBuilder("magenta", "Magenta", Team.Colour.MAGENTA),
    };

}
