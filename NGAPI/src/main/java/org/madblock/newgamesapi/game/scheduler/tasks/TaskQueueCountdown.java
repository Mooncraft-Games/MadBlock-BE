package org.madblock.newgamesapi.game.scheduler.tasks;

import cn.nukkit.Player;
import cn.nukkit.scheduler.Task;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.game.GameHandler;

public class TaskQueueCountdown extends Task {

    private String access_token;
    private GameHandler handler;

    public TaskQueueCountdown(String access_token, GameHandler handler){
        this.access_token = access_token;
        this.handler = handler;
    }

    @Override
    public void onRun(int currentTick) {
        if(handler.getGameState() == GameHandler.GameState.PRE_COUNTDOWN ){

            if(handler.getGameID().getGameProperties().isTourneyGamemode() && !handler.isTourneyStarted()){
                NewGamesAPI1.get().getServer().getScheduler().scheduleDelayedTask(new TaskQueueCountdown(access_token, handler), 2);
                return;
            }

            int defaultGameCountdown = handler.getGameID().getGameProperties().getDefaultCountdownLength();
            int mapOverrideTimer = handler.getPrimaryMapID().getIntegers().getOrDefault("countdown", defaultGameCountdown);

            for(Player player : handler.getPlayers()) executePerPlayerActivites(player, handler);
            for(Player player : handler.getTourneyMasters()) executePerPlayerActivites(player, handler);

            handler.setGameState(access_token, GameHandler.GameState.COUNTDOWN);
            handler.getGameBehaviors().initialCountdown();
            NewGamesAPI1.get().getServer().getScheduler().scheduleRepeatingTask(new TaskTickdownCountdown(access_token, handler, mapOverrideTimer+1), 20);
        }
    }

    public static void executePerPlayerActivites(Player player, GameHandler handler){
        player.sendMessage(handler.getGameID().getGameInfoMessage());
        player.clearTitle();
    }
}
