package org.madblock.newgamesapi.game.internal.hub.regions;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import cn.nukkit.math.BlockVector3;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.game.internal.hub.GameBehaviorLobby;
import org.madblock.newgamesapi.kits.Kit;
import org.madblock.newgamesapi.map.functionalregions.FunctionalRegionCallData;
import org.madblock.newgamesapi.map.types.MapRegion;
import org.madblock.newgamesapi.team.Team;
import org.madblock.ranks.api.RankManager;
import org.madblock.ranks.api.RankProfile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class RegionTagHubBuildArea implements Consumer<FunctionalRegionCallData> {

    protected GameHandler handler;
    protected String buildingTeamID;
    protected String returnTeamID;

    protected HashMap<MapRegion, Level> allowedRegionsCache;

    protected int lastTick;

    public RegionTagHubBuildArea(GameHandler handler, String buildTeamID, String returnTeamID) {
        this.handler = handler;
        this.buildingTeamID = buildTeamID;
        this.returnTeamID = returnTeamID;
        this.allowedRegionsCache = new HashMap<>();
        this.lastTick = NewGamesAPI1.get().getServer().getTick();
    }

    @Override
    public void accept(FunctionalRegionCallData data) {
        this.allowedRegionsCache.put(data.getRegion(), data.getLevel());
        if (NewGamesAPI1.get().getServer().getTick() != lastTick){
            this.lastTick = NewGamesAPI1.get().getServer().getTick(); // Prevents multiple build regions from executing.

            for (Player player : data.getLevel().getPlayers().values()) {
                Optional<Team> playerTeam = handler.getPlayerTeam(player);
                playerTeam.ifPresent(team -> {

                    if(!team.getId().equals(GameBehaviorLobby.LOBBY_SUPER_TEAM_ID)) { // Super team is immune
                        boolean withinRegion = false;

                        for(MapRegion region: allowedRegionsCache.keySet()) {

                            if((player.getLevel() == allowedRegionsCache.get(region)) && region.isWithinThisRegion(player.getPosition())){
                                withinRegion = true;
                                break;
                            }
                        }


                        Kit defaultKit = handler.getGameID().getGameKits().getDefaultKit();
                        Optional<RankProfile> rankProfile = RankManager.getInstance().getRankProfile(player);

                        if (rankProfile.isPresent() && rankProfile.get().hasPermission("lobby.build_area.build")) {

                            if (withinRegion) {

                                if (!team.getId().equals(GameBehaviorLobby.LOBBY_BUILDER_TEAM_ID)) {
                                    Utility.setGamemodeWorkaround(player, Player.CREATIVE, false, null);
                                    handler.switchPlayerToTeam(player, handler.getTeams().get(buildingTeamID), team, false);
                                    handler.equipPlayerKit(player, handler.getGameID().getGameKits().getGroupKits().getOrDefault(GameBehaviorLobby.LOBBY_BUILDER_KIT_ID, defaultKit), true);
                                }

                            } else {

                                if (team.getId().equals(GameBehaviorLobby.LOBBY_BUILDER_TEAM_ID)) {
                                    player.setGamemode(Player.SURVIVAL);
                                    Utility.setGamemodeWorkaround(player, Player.SURVIVAL, false, null);
                                    handler.switchPlayerToTeam(player, handler.getTeams().get(returnTeamID), team, false);
                                    handler.equipPlayerKit(player, defaultKit, true);
                                }
                            }
                        }
                    }

                });
            }

        }
    }

    public boolean isLegalPlacement(Level level, BlockVector3 vector3){
        for(Map.Entry<MapRegion, Level> entry: allowedRegionsCache.entrySet()){
            if((entry.getValue() == level) && (entry.getKey().isWithinThisRegion(vector3.asVector3()))){
                return true;
            }
        }
        return false;
    }
}
