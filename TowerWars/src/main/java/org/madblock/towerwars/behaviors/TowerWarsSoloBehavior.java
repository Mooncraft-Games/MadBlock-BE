package org.madblock.towerwars.behaviors;

import cn.nukkit.Player;
import org.madblock.newgamesapi.team.Team;
import org.madblock.towerwars.utils.GameRegion;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TowerWarsSoloBehavior extends TowerWarsBehavior {

    private final Map<UUID, Integer> lives;
    private final Map<UUID, Integer> balances;

    public TowerWarsSoloBehavior() {
        super();
        this.lives = new HashMap<>();
        this.balances = new HashMap<>();
    }

    @Override
    public Team.GenericTeamBuilder[] getTeams() {
        return new Team.GenericTeamBuilder[]{
                Team.newBasicTeamBuilder("players", "Players", Team.Colour.BLUE)
                        .setFlightEnabled(true)
                        .setCanDealDamage(false)
                        .setCanPlayersDropItems(false)
        };
    }

    @Override
    public int getLives(Player player) {
        return this.lives.getOrDefault(player.getUniqueId(), this.getInitialLives());
    }

    @Override
    public int getBalance(Player player) {
        return this.balances.getOrDefault(player.getUniqueId(), this.getInitialBalance());
    }

    @Override
    public void setLives(Player player, int lives) {
        this.lives.put(player.getUniqueId(), lives);
    }

    @Override
    public void setBalance(Player player, int balance) {
        this.balances.put(player.getUniqueId(), balance);
    }

    @Override
    public GameRegion getPlayerGameRegion(Player player) {
        return null;    // TODO: Retrieve region from mapid based off of player team
    }
}
