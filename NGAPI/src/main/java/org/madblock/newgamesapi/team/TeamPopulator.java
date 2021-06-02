package org.madblock.newgamesapi.team;

import cn.nukkit.Player;
import org.madblock.newgamesapi.game.GameHandler;

import java.util.ArrayList;

/**
 * Should be updated whenever teams are added or removed.
 */
public class TeamPopulator {

    private GameHandler handler;
    private ArrayList<String> teams;

    public TeamPopulator(GameHandler handler){
        this.handler = handler;
        this.teams = new ArrayList<>();
        this.updateCache();
    }

    public void updateCache(){
        for(Team team: handler.getTeams().values()) {
            if (team.isActiveGameTeam() && team.doesParticipateInTeamBalancing()) {
                teams.add(team.getId());
            }
        }
    }

    /**
     * @return TeamID for the team most in need of players.
     * @throws IllegalStateException - If the team list is outdated.
     */
    public String getPriorityFillTeam() throws IllegalStateException {
        String prioritizeID = "";
        Integer lowestCount = null;
        for(String id: teams){
            if(!handler.getTeams().containsKey(id)) throw new IllegalStateException("Teams have changed since TeamPopulator was instantiated. Please don't recycle them, it's not a good idea.");
            Team team = handler.getTeams().get(id);
            if((lowestCount == null) || team.getPlayers().size() <= lowestCount){
                lowestCount = team.getPlayers().size();
                prioritizeID = team.getId();
            }
        }
        return prioritizeID;
    }

    /**
     * @param player - The player you want sorting into a team.
     * @return If the player was switched to the team.
     * @throws IllegalArgumentException - thrown if the player specified was not part of the game
     */
    public String switchPlayerToInDemandTeam(Player player){
        if(!handler.getPlayers().contains(player)) throw new IllegalArgumentException("Player must be part of the game to be assigned a team");
        String teamDemand = getPriorityFillTeam();
        handler.switchPlayerToTeam(player, handler.getTeams().get(teamDemand));
        return teamDemand;
    }

}
