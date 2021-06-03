package org.madblock.newgamesapi.map.functionalregions.defaults;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.game.deaths.DeathCategory;
import org.madblock.newgamesapi.game.deaths.DeathCause;
import org.madblock.newgamesapi.game.deaths.DeathSubCategory;
import org.madblock.newgamesapi.game.internal.hub.GameBehaviorLobby;
import org.madblock.newgamesapi.map.types.MapRegion;
import org.madblock.newgamesapi.team.Team;

public class TagBehaviorDeathbox extends TagBehaviorCollisionbox {

    public TagBehaviorDeathbox(GameHandler handler) {
        super(handler);
    }

    @Override
    public void execute(Player player, Team team, MapRegion mapRegion, Level level) {
        deathboxTrigger(mapRegion, player, team, handler);
    }

    protected static void deathboxTrigger(MapRegion self, Player player, Team team, GameHandler handler) {
        if(handler.getPlayers().contains(player)) {

            if(handler.getGameBehaviors() instanceof GameBehaviorLobby) {
                if(((GameBehaviorLobby) handler.getGameBehaviors()).getSuperPlayers().contains(player)) {
                    return;
                }
            }

            DeathCause deathCause = DeathCause.builder(player, team, DeathCategory.ENVIRONMENT, DeathSubCategory.BORDER, 9999f).setKillingRegion(self).build();
            handler.getDeathManager().killPlayer(deathCause, true);
        }
    }

}
