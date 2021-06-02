package org.madblock.newgamesapi.map.functionalregions.defaults;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.map.types.MapRegion;
import org.madblock.newgamesapi.team.Team;

public class TagBehaviorInvertedDeathbox extends TagBehaviorInvertedCollisionbox {

    public TagBehaviorInvertedDeathbox(GameHandler handler) { super(handler); }

    @Override
    public void execute(Player player, Team team, MapRegion mapRegion, Level level) {
        TagBehaviorDeathbox.deathboxTrigger(mapRegion, player, team, getHandler());
    }


}
