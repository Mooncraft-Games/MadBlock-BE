package org.madblock.skywars.pointentities;

import cn.nukkit.block.Block;
import cn.nukkit.math.Vector3;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.map.MapID;
import org.madblock.newgamesapi.map.pointentities.PointEntityCallData;
import org.madblock.newgamesapi.map.pointentities.PointEntityType;
import org.madblock.newgamesapi.map.types.MapRegion;
import org.madblock.newgamesapi.map.types.PointEntity;
import org.madblock.skywars.utils.Constants;
import org.madblock.skywars.utils.SkywarsUtils;

import java.util.HashMap;
import java.util.Map;

public class PointEntityTypeCorruption extends PointEntityType {

    public static String POINT_ENTITY_TYPE = "corruption";

    private Map<PointEntity, Integer> currentCorruptionRadius;

    public PointEntityTypeCorruption(GameHandler gameHandler) {
        super(POINT_ENTITY_TYPE, gameHandler);
        this.currentCorruptionRadius = new HashMap<>();
    }

    @Override
    public void onRegister () {
        this.addFunction("apply_corruption", this::generateCorruption);

    }

    @Override
    public void onAddPointEntity (PointEntity pointEntity) {
        super.onAddPointEntity(pointEntity);
        this.currentCorruptionRadius.put(pointEntity, pointEntity.getIntegerProperties().getOrDefault("radius", Constants.DEFAULT_CORRUPTION_RADIUS));
    }

    public int getCurrentCorruptionRadius (PointEntity pointEntity) {
        return this.currentCorruptionRadius.get(pointEntity);
    }

    private void generateCorruption (PointEntityCallData data) {
        MapRegion playZoneRegion = this.getGameHandler().getPrimaryMapID().getRegions().get("play_zone");;
        int currentRadius = this.getCurrentCorruptionRadius(data.getPointEntity());
        for (int angle = 0; angle <= 360; angle += 1) {
            int x = (int)Math.round(Math.cos(angle * Math.PI / 180) * currentRadius + data.getPointEntity().getX());
            int z = (int)Math.round(Math.sin(angle * Math.PI / 180) * currentRadius + data.getPointEntity().getZ());
            for (int y = playZoneRegion.getPosLesser().getY(); y <= playZoneRegion.getPosGreater().getY(); y++) {
                if (data.getLevel().getBlock(new Vector3(x, y, z)).getId() != Block.AIR) {
                    data.getLevel().setBlock(new Vector3(x, y, z), SkywarsUtils.getRandomCorruptionBlock(), false, false);
                }
                if (data.getLevel().getBlock(new Vector3(x, y, z - 1)).getId() != Block.AIR) {
                    data.getLevel().setBlock(new Vector3(x, y, z - 1), SkywarsUtils.getRandomCorruptionBlock(), false, false);
                }

            }
        }
        this.currentCorruptionRadius.put(data.getPointEntity(), this.currentCorruptionRadius.get(data.getPointEntity()) - 1);
    }


}
