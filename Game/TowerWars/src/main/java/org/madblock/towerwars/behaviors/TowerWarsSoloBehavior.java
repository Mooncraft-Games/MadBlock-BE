package org.madblock.towerwars.behaviors;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.player.PlayerChatEvent;
import org.madblock.newgamesapi.team.Team;
import org.madblock.towerwars.enemies.enemy.Enemy;
import org.madblock.towerwars.enemies.types.SilverfishEnemyType;
import org.madblock.towerwars.utils.GameRegion;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TowerWarsSoloBehavior extends TowerWarsBehavior {

    private final Map<UUID, Integer> lives = new HashMap<>();
    private final Map<UUID, Integer> balances = new HashMap<>();


    @EventHandler
    public void test(PlayerChatEvent event) {
        Enemy enemy = this.getEnemyRegistry().getEnemyType(SilverfishEnemyType.ID)
                .create(this.getPlayerGameRegion(event.getPlayer()));
        enemy.spawn(event.getPlayer().getPosition());
        this.addEnemy(enemy);
        event.getPlayer().sendMessage("spawned");
    }

    @Override
    public Team.GenericTeamBuilder[] getTeams() {
        return new Team.GenericTeamBuilder[]{
                generateTeam("blue", "Blue", Team.Colour.BLUE),
                generateTeam("red", "Red", Team.Colour.RED),
                generateTeam("green", "Green", Team.Colour.GREEN),
                generateTeam("yellow", "Yellow", Team.Colour.YELLOW),
                generateTeam("orange", "Orange", Team.Colour.ORANGE),
                generateTeam("cyan", "Cyan", Team.Colour.CYAN)
        };
    }

    private static Team.GenericTeamBuilder generateTeam(String id, String name, Team.Colour teamColour) {
        return Team.newBasicTeamBuilder(id, name, teamColour)
                .setFlightEnabled(true)
                .setCanDealDamage(false)
                .setCanPlayersDropItems(false);
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
        Team team = this.getSessionHandler()
                .getPlayerTeam(player)
                .filter(Team::isActiveGameTeam)
                .orElse(null);

        if (team == null) {
            return null;
        }
        return this.gameRegions.getOrDefault(team.getId(), null);
    }

    @Override
    public Player getGameRegionOwner(GameRegion gameRegion) {
        return null;
    }
}