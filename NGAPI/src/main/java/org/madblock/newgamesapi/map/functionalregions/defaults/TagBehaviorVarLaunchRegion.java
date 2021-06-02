package org.madblock.newgamesapi.map.functionalregions.defaults;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import cn.nukkit.level.Sound;
import cn.nukkit.math.Vector3;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.map.types.MapRegion;
import org.madblock.newgamesapi.team.Team;

/**
 * Launches the player in their view direction with a given force.
 */
public class TagBehaviorVarLaunchRegion extends TagBehaviorCollisionbox {

    public TagBehaviorVarLaunchRegion(GameHandler handler) {
        super(handler);
    }

    @Override
    public void execute(Player player, Team team, MapRegion mapRegion, Level level) {
        float force = 1.8f;
        boolean makeSound = true;
        boolean enableYTweak = true;

        for (String tag: mapRegion.getTags()) {
            if (tag.equalsIgnoreCase("silent")) {
                makeSound = false;
                continue;
            }

            if (tag.equalsIgnoreCase("disableYTweak")) {
                enableYTweak = false;
                continue;
            }

            // Attempt to parse any number as a float.
            try {
                force = Float.parseFloat(tag);
            } catch (Exception err) {} // silent
        }

        if(makeSound) player.getLevel().addSound(player.getPosition(), Sound.BLOCK_BEEHIVE_ENTER);
        Vector3 dir = player.getDirectionVector();
        float tweak = ((dir.y < 0) && enableYTweak ) ? 0.65f : 1f;
        player.setMotion(new Vector3(dir.x, Math.abs(dir.y * tweak), dir.z).multiply(force));

    }

}
