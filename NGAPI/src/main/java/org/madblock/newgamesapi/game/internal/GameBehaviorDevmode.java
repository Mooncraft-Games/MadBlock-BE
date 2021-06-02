package org.madblock.newgamesapi.game.internal;

import cn.nukkit.Player;
import cn.nukkit.math.Vector3;
import org.madblock.newgamesapi.game.GameBehavior;
import org.madblock.newgamesapi.game.events.GamePlayerDeathEvent;
import org.madblock.newgamesapi.rewards.AchievementProgressChunk;
import org.madblock.newgamesapi.rewards.RewardChunk;
import org.madblock.newgamesapi.team.Team;
import org.madblock.newgamesapi.team.TeamPresets;

import java.util.ArrayList;

public class GameBehaviorDevmode extends GameBehavior {

    private int deathstate = 0;

    private ArrayList<Player> deathLog = new ArrayList<Player>();

    @Override
    public void onInitialCountdownEnd() {
    }

    @Override
    public void registerGameSchedulerTasks() {
        //Runs every 3 seconds after a 1 second wait.
        getSessionHandler().getGameScheduler().registerGameTask(this::randomChatSpam, 1, 60);

        //Throws player into the air every 20 seconds after 5 seconds.
        getSessionHandler().getGameScheduler().registerGameTask(this::flingPlayers, 100, 400);

        //Checks if game teams are empty
        getSessionHandler().getGameScheduler().registerGameTask(this::checkGameStatus, 0, 5);
    }

    @Override
    public void onGameMiscDeathEvent(GamePlayerDeathEvent event) {

        switch (deathstate){
            case 0:
                event.setDeathState(GamePlayerDeathEvent.DeathState.INSTANT_RESPAWN);
                deathstate++;
                break;
            case 1:
                event.setDeathState(GamePlayerDeathEvent.DeathState.TIMED_RESPAWN);
                deathstate++;
                break;
            case 2:
                event.setDeathState(GamePlayerDeathEvent.DeathState.MOVE_TO_DEAD_SPECTATORS);
                deathstate = 0;
                break;
        }

        event.getDeathCause().getVictim().sendMessage(event.getDeathState().toString());

        deathLog.remove(event.getDeathCause().getVictim());
        deathLog.add(0, event.getDeathCause().getVictim());
    }

    public void checkGameStatus(){
        Team spectators = getSessionHandler().getTeams().get(TeamPresets.SPECTATOR_TEAM_ID);
        Team dead = getSessionHandler().getTeams().get(TeamPresets.DEAD_TEAM_ID);
        if(getSessionHandler().getPlayers().size() == (spectators.getPlayers().size() + dead.getPlayers().size())){
            Player[] top3deaths = new Player[]{ null, null, null};
            for(int i = 0; (i < deathLog.size()) && (i < 3); i++){
                top3deaths[i] = deathLog.get(i);
            }
            getSessionHandler().declareVictoryInPlayerOrder(top3deaths[0], top3deaths[1], top3deaths[2]);
        }
    }

    public void randomChatSpam(){
        for(Team team: getSessionHandler().getTeams().values()){
            if(team.isActiveGameTeam()) {
                team.getPlayers().forEach( player -> {
                    player.sendMessage("This is a message");
                    getSessionHandler().addRewardChunk(player, new RewardChunk("spam", "*discord ping*", 5, 1));
                    getSessionHandler().addRewardChunk(player, new AchievementProgressChunk("ac_spam", "You got mail!", 2, 1, 1, 10000));
                });
            }
        }
    }

    public void flingPlayers(){
        for(Team team: getSessionHandler().getTeams().values()){
            if(team.isActiveGameTeam()) {
                for (Player player : team.getPlayers()) {
                    player.setMotion(player.getMotion().add(new Vector3(0, 1.5, 0)));
                }
            }
        }
    }

}
