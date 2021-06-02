package org.madblock.newgamesapi.game;

import cn.nukkit.Player;
import cn.nukkit.event.Listener;
import org.madblock.newgamesapi.game.events.GamePlayerDeathEvent;
import org.madblock.newgamesapi.kits.Kit;
import org.madblock.newgamesapi.team.Team;
import org.madblock.newgamesapi.team.TeamPresets;

import java.util.Optional;

public abstract class GameBehavior implements Listener {

    private GameHandler sessionHandler;

    public final void setSessionHandler(GameHandler sessionHandler){
        if(this.sessionHandler == null) {
            this.sessionHandler = sessionHandler;
        }
    }

    // --- Game Settings ---

    /** Returns the teams to be used by the game. Teams can be generated dynamically. */
    public Team.GenericTeamBuilder[] getTeams(){ return TeamPresets.FOUR_TEAMS; }

    // --- Game Events ---
    /** Game has "begun" but there may be a countdown period if enabled.
     * @return Time to wait before entering the countdown <b>in ticks.</b>
     */
    public int onGameBegin() { return 10; }

    public void onPlayerLeaveGame(Player player){ }
    public void onAddPlayerToTeam(Player player, Team team){ }
    public void onRemovePlayerFromTeam(Player player, Team team){ }

    /**
     * Called when a player joins the game before an active round or it's countdown has started.
     * @return the team the player should be placed on and spawned in. Leave empty for the game to sort it.
     */
    public Optional<Team> onPreGameJoinEvent(Player player) { return Optional.empty(); }

    //IF CountdownEnabled
    /** Countdown has begun. */
    public void initialCountdown() { }
    /**
     * Called when a player joins the game during the countdown.
     * @return the team the player should be placed on and spawned in. Leave empty for the game to sort it.
     */
    public Optional<Team> onCountdownJoinEvent(Player player) { return Optional.empty(); }
    /** Countdown has ended. This should be the part where players are allowed to move. */
    public abstract void onInitialCountdownEnd();
    //END
    /** Called when a kit is equipped.*/
    public void onKitEquip(Player player, Kit kit) { }
    //WHILE GameLoopIsActive
    /** Game has entered it's main loop. Register any tasks you want run. It is highly recommended you create some
     * type of repeating task else the game will not end. */
    public abstract void registerGameSchedulerTasks();
    /** Called when a player dies to a block */
    public void onGameDeathByBlock(GamePlayerDeathEvent event) { }
    /** Called when a player dies to an entity */
    public void onGameDeathByEntity(GamePlayerDeathEvent event) { }
    /** Called when a player dies to another player */
    public void onGameDeathByPlayer(GamePlayerDeathEvent event) { }
    /** Called when a player dies but the reason is not by an entity, player, or block.*/
    public void onGameMiscDeathEvent(GamePlayerDeathEvent event) { }
    /** Called when a player runs /super. */
    public void onSuper(Player player) { }

    /**
     * Called when a player joins the game during an active round.
     * @return the team the player should be placed on and spawned in. Leave empty for the game to sort it.
     */
    public Optional<Team> onMidGameJoinEvent(Player player) {
        return Optional.ofNullable(getSessionHandler().getTeams().get("spectators"));
    }
    //END
    /**
     * Used for custom win effects for a whole team. Does not override default win behaviors. By
     * default it calls #onVictory(Player[]);
     */
    public void onVictory(Team winningteam) { onVictory(winningteam.getPlayers().toArray(new Player[0])); }
    /**
     * Used for custom win effects for a player (or multiple if there's a tie). Does not override
     * default win behaviors.
     */
    public void onVictory(Player[] winningplayer) { }

    /** Clean up anything other than worlds as the game has finished. */
    public void cleanUp() { }

    public GameHandler getSessionHandler() {
        return sessionHandler;
    }
}
