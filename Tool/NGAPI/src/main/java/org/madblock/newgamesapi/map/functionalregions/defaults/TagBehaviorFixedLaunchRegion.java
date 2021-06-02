package org.madblock.newgamesapi.map.functionalregions.defaults;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import cn.nukkit.level.Sound;
import cn.nukkit.math.Vector3;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.map.types.MapRegion;
import org.madblock.newgamesapi.team.Team;

/**
 * Launches the player in a specific direction.
 */
public class TagBehaviorFixedLaunchRegion extends TagBehaviorCollisionbox {

    public TagBehaviorFixedLaunchRegion(GameHandler handler) {
        super(handler);
    }

    @Override
    public void execute(Player player, Team team, MapRegion mapRegion, Level level) {
        float[] forces = new float[] {0, 0, 1.8f}; // formatted x, z, y as Y is somewhat less important;
        int foundForces = 0;
        boolean makeSound = true;

        for (String tag: mapRegion.getTags()) {
            if (tag.equalsIgnoreCase("silent")) {
                makeSound = false;
                continue;
            }

            if(foundForces < 3) {
                // Attempt to parse any number as a float.
                try {
                    forces[foundForces] = Float.parseFloat(tag);
                    foundForces++;
                } catch (Exception err) {
                } // silent
            }
        }

        if(makeSound) player.getLevel().addSound(player.getPosition(), Sound.BLOCK_BEEHIVE_ENTER);
        player.setMotion(new Vector3(forces[0], forces[1], forces[2]));

    }

}
