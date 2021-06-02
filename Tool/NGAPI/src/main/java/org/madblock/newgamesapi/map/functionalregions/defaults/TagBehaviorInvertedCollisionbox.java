package org.madblock.newgamesapi.map.functionalregions.defaults;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.map.types.MapRegion;

public abstract class TagBehaviorInvertedCollisionbox extends TagBehaviorCollisionbox{

    public TagBehaviorInvertedCollisionbox(GameHandler handler) {
        super(handler);
    }

    @Override
    protected boolean checkCollision(MapRegion region, Level level, Player player){
        return (player.getLevel() == level) && !region.isWithinThisRegion(player.getPosition());
    }

}
