package org.madblock.newgamesapi.game.scheduler.tasks;

import cn.nukkit.Player;
import cn.nukkit.scheduler.Task;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.team.SpectatingTeam;
import org.madblock.newgamesapi.team.Team;

import java.util.Optional;

public class TaskStartSessionLoops extends Task {

    private String access_token;
    private GameHandler handler;

    public TaskStartSessionLoops(String access_token, GameHandler handler){
        this.access_token = access_token;
        this.handler = handler;
    }

    @Override
    public void onRun(int currentTick) {
        if(handler.getGameState() == GameHandler.GameState.PRE_MAIN_LOOP){

            for(Player player: handler.getPlayers()) preparePlayer(player, handler);
            for(Player player: handler.getTourneyMasters()) player.sendMessage(handler.getPrimaryMapID().getMapInfoMessage());

            handler.setGameState(access_token, GameHandler.GameState.MAIN_LOOP);
            handler.enterMainLoop(access_token);
            handler.getGameBehaviors().registerGameSchedulerTasks();
        }
    }

    public static boolean preparePlayer(Player player, GameHandler handler){
        Optional<Team> playerTeam = handler.getPlayerTeam(player);
        if(playerTeam.isPresent() && !(playerTeam.get() instanceof SpectatingTeam)) {
            player.setImmobile(false);
            player.sendMessage(handler.getPrimaryMapID().getMapInfoMessage());
            return true;
        }
        return false;
    }

}
