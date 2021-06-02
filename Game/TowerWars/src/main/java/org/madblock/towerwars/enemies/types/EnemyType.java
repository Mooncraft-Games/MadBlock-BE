package org.madblock.towerwars.enemies.types;

import org.madblock.newgamesapi.map.types.MapRegion;
import org.madblock.towerwars.enemies.enemy.Enemy;
import org.madblock.towerwars.enemies.enemy.EnemyProperties;

public abstract class EnemyType {

    private final EnemyProperties properties;

    public EnemyType(EnemyProperties properties) {
        this.properties = properties;
    }

    public abstract String getId();

    public abstract Enemy create(MapRegion playArea);

    public EnemyProperties getProperties() {
        return this.properties;
    }

}
