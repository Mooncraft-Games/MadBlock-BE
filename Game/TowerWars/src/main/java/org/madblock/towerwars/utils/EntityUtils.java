package org.madblock.towerwars.utils;

import cn.nukkit.entity.Entity;
import cn.nukkit.math.Vector3;

public class EntityUtils {

    public static void lookAt(Entity entity, Entity target) {
        lookAt(entity, target.getPosition());
    }

    public static void lookAt(Entity entity, Vector3 target) {
        Vector3 normalizedDirection = entity.getLocation().subtract(target).normalize();
        entity.setRotation(
                Math.atan2(normalizedDirection.getZ(), normalizedDirection.getX()) * 180 / Math.PI + 90,
                Math.asin(normalizedDirection.getY()) * 180 / Math.PI
        );
    }

}