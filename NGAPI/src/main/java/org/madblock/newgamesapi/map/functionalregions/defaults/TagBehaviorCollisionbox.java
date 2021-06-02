package org.madblock.newgamesapi.map.functionalregions.defaults;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.map.functionalregions.FunctionalRegionCallData;
import org.madblock.newgamesapi.map.functionalregions.FunctionalRegionManager;
import org.madblock.newgamesapi.map.types.MapRegion;
import org.madblock.newgamesapi.team.DeadTeam;
import org.madblock.newgamesapi.team.SpectatingTeam;
import org.madblock.newgamesapi.team.Team;

import java.util.ArrayList;
import java.util.function.Consumer;

public abstract class TagBehaviorCollisionbox implements Consumer<FunctionalRegionCallData> {

    protected GameHandler handler;

    public TagBehaviorCollisionbox(GameHandler handler) {
        this.handler = handler;
    }

    @Override
    public void accept(FunctionalRegionCallData data) {
        MapRegion region = data.getRegion();
        Level level = data.getLevel();
        FunctionalRegionManager m = handler.getFunctionalRegionManager();
        for(Team team: handler.getTeams().values()){

            if(team instanceof SpectatingTeam){
                m.getRegionFunctionForTag("spectator_collider").ifPresent(f -> {
                    if(f.getRegionsCopy().containsKey(region)) teamCheckExecution(team, region, level);
                });
            }
            if(team instanceof DeadTeam){
                m.getRegionFunctionForTag("dead_collider").ifPresent(f -> {
                    if(f.getRegionsCopy().containsKey(region)) teamCheckExecution(team, region, level);
                });
            }
            if(team.isActiveGameTeam()){
                m.getRegionFunctionForTag("active_player_collider").ifPresent(f -> {
                    if(f.getRegionsCopy().containsKey(region)) teamCheckExecution(team, region, level);
                });
            }
            m.getRegionFunctionForTag("conditional_team_collider").ifPresent(f -> {
                if(f.getRegionsCopy().containsKey(region)) {
                    for(FunctionalRegionCallData teamCallData: f.getRegionsCopy().get(region)) {
                        String[] arguments = teamCallData.getArgs();
                        if (arguments.length >= 1) {
                            String teamid = arguments[0].toLowerCase();
                            boolean isInclusionalCondition;
                            if (arguments.length >= 2) {
                                isInclusionalCondition = FunctionalRegionCallData.parseBoolean(arguments[1]).orElse(true);
                            } else {
                                isInclusionalCondition = true;
                            }
                            if (isInclusionalCondition) {
                                if (teamid.equals(team.getId().toLowerCase())) {
                                    teamCheckExecution(team, region, level);
                                }
                            } else {
                                if (!teamid.equals(team.getId().toLowerCase())) {
                                    teamCheckExecution(team, region, level);
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    private void teamCheckExecution(Team team, MapRegion region, Level level){
        for(Player player: new ArrayList<>(team.getPlayers())){
            if(checkCollision(region, level, player)){
                execute(player, team, region, level);
            }
        }
    }

    public abstract void execute(Player player, Team team, MapRegion mapRegion, Level level);

    protected boolean checkCollision(MapRegion region, Level level, Player player){
        return (player.getLevel() == level) && region.isWithinThisRegion(player.getPosition());
    }

    public GameHandler getHandler() {
        return handler;
    }
}
