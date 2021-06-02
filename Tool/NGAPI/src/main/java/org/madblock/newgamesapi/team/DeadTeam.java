package org.madblock.newgamesapi.team;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import org.madblock.newgamesapi.game.GameHandler;

import java.util.HashMap;

public class DeadTeam extends SpectatingTeam {

    protected HashMap<Player, Team> previousTeams;

    protected DeadTeam(GameHandler handler, String id, String displayname, Colour colour, boolean isTeamNameDisplayed, boolean canPlaceAndBreakBlocks, boolean canDropItems, boolean canPlayersInteractWithBlocks, boolean canPickUpItems, boolean isVisible, boolean isFlightEnabled, boolean isNoClipEnabled) {
        super(handler, id, displayname, colour, isTeamNameDisplayed, canPlaceAndBreakBlocks, canDropItems, canPlayersInteractWithBlocks, canPickUpItems, isVisible, isFlightEnabled, isNoClipEnabled);
        this.previousTeams = new HashMap<>();
    }

    public boolean addPlayerToTeamAsDead(Player player, Team previousTeam){
        previousTeams.put(player, previousTeam);
        return super.addPlayerToTeam(player);
    }
    /**
     * Removes the specified player from the team and removes name colour.
     *  @return true if player was found on team.
     */
    public boolean removePlayerFromTeam(Player player) {
        if(super.removePlayerFromTeam(player)){
            previousTeams.remove(player);
            return true;
        }
        return false;
    }

    /**
     * Revives a dead player and puts them on their past team.
     *  @return true if player had a logged previous team and was revived.
     */
    public boolean revivePlayer(Player player){
        if(players.contains(player) && previousTeams.containsKey(player)){
            Team prevTeam = previousTeams.get(player);
            removePlayerFromTeam(player);
            player.setHealth(player.getMaxHealth());
            return prevTeam.addPlayerToTeam(player);
        }
        return false;
    }

    public void overwritePreviousTeam(Player player, Team newTeam) {
        if(previousTeams.containsKey(player)){
            previousTeams.put(player, newTeam);
        }
    }

    public HashMap<Player, Team> getPreviousTeams() {
        return new HashMap<>(previousTeams);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        super.onDamage(event);
    }

    public static DeadTeamBuilder newDeadTeamBuilder(String id, String teamname, Colour colour){
        return new DeadTeamBuilder(id, teamname, colour);
    }

    public static class DeadTeamBuilder extends SpectatorBuilder {

        public DeadTeamBuilder(String id, String displayName, Colour colour) {
            super(id, displayName, colour);
        }

        @Override
        public DeadTeam build(GameHandler handler){
            verify(handler);
            DeadTeam team = new DeadTeam(
                    handler,
                    id,
                    displayName,
                    colour,
                    isTeamNameDisplayed,
                    canPlaceAndBreakBlocks,
                    canPlayersDropItems,
                    canPlayersInteractWithBlocks,
                    canPlayersPickUpItems,
                    arePlayersVisible,
                    isFlightEnabled,
                    isNoClipEnabled
            );
            team.activate();
            return team;
        }

    }

}
