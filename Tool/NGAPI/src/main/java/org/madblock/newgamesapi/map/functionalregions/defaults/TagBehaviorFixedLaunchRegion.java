package org.madblock.newgamesapi.map.functionalregions.defaults;

import cn.nukkit.Player;
import cn.nukkit.level.Level;
import cn.nukkit.level.Sound;
import cn.nukkit.math.Vector3;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.map.functionalregions.FunctionalRegionCallData;
import org.madblock.newgamesapi.map.pointentities.PointEntityType;
import org.madblock.newgamesapi.map.types.MapRegion;
import org.madblock.newgamesapi.map.types.PointEntity;
import org.madblock.newgamesapi.team.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Launches the player in a specific direction.
 */
public class TagBehaviorFixedLaunchRegion extends TagBehaviorCollisionbox {

    public static final int TICK_INTERVAL = 3;

    protected HashMap<MapRegion, AtomicInteger> cooldownTicks;

    public TagBehaviorFixedLaunchRegion(GameHandler handler) {
        super(handler);
        this.cooldownTicks = new HashMap<>();
    }

    @Override
    public void accept(FunctionalRegionCallData data) {
        super.accept(data);

        for(Map.Entry<MapRegion, AtomicInteger> ticks : new HashMap<>(this.cooldownTicks).entrySet()) {
            int val = ticks.getValue().addAndGet(-TICK_INTERVAL);
            if(val <= 0) cooldownTicks.remove(ticks.getKey());
        }
    }

    @Override
    public void execute(Player player, Team team, MapRegion mapRegion, Level level) {
        if(cooldownTicks.containsKey(mapRegion)) return;

        String paramType = "";
        float[] forces = new float[] {0, 0, 1.8f}; // formatted x, z, y as Y is somewhat less important;
        int foundForces = 0;
        boolean makeSound = true;

        String animatorName = null;
        String animationName = null;
        String controllerName = null;

        for (String tag: mapRegion.getTags()) {
            switch (paramType) {

                case "animator":
                    paramType = "";
                    animatorName = tag;
                    break;

                case "animation":
                    paramType = "";
                    animationName = tag;
                    break;

                case "controller":
                    paramType = "";
                    controllerName = tag;
                    break;

                case "cooldown":
                    paramType = "";
                    cooldownTicks.put(mapRegion, new AtomicInteger(Integer.parseInt(tag)));
                    break;

                default:
                    if(tag.equalsIgnoreCase("animator:")) {
                        paramType = "animator";
                        continue;
                    }

                    if(tag.equalsIgnoreCase("animation:")) {
                        paramType = "animation";
                        continue;
                    }

                    if(tag.equalsIgnoreCase("controller:")) {
                        paramType = "controller";
                        continue;
                    }

                    if(tag.equalsIgnoreCase("cooldown:")) {
                        paramType = "cooldown";
                        continue;
                    }

                    if (tag.equalsIgnoreCase("silent")) {
                        makeSound = false;
                        continue;
                    }

                    if (foundForces < 3) {
                        // Attempt to parse any number as a float.
                        try {
                            forces[foundForces] = Float.parseFloat(tag);
                            foundForces++;
                        } catch (Exception ignored) { } // silent
                    }
                    break;
            }
        }

        if(makeSound) player.getLevel().addSound(player.getPosition(), Sound.TILE_PISTON_OUT, 1, 0.5f);
        player.setMotion(new Vector3(forces[0], forces[1], forces[2]));

        if(animatorName != null) {
            PointEntity entity = this.getHandler().getPointEntityTypeManager().getGlobalLookup().get(animatorName);

            if(entity != null) {
                PointEntityType type = this.getHandler().getPointEntityTypeManager().getRegisteredTypes().get(entity.getType());

                HashMap<String, String> params = new HashMap<>();
                if(animationName != null) params.put("animation", animationName);
                if(controllerName != null) params.put("controller", controllerName);

                type.executeFunction("animate", entity, this.getHandler().getPrimaryMap(), params);
            }
        }

    }

}
