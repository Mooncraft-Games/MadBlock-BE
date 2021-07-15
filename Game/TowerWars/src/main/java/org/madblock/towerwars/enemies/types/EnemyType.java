package org.madblock.towerwars.enemies.types;

import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.enemies.enemy.Enemy;
import org.madblock.towerwars.enemies.enemy.EnemyProperties;
import org.madblock.towerwars.utils.GameRegion;

public abstract class EnemyType {

    protected final TowerWarsBehavior behavior;

    public EnemyType(TowerWarsBehavior behavior) {
        this.behavior = behavior;
    }

    public abstract String getId();
    public abstract String getName();
    public abstract String getDescription();
    public abstract EnemyProperties getProperties();

    public abstract int getCost();

    public abstract Enemy create(GameRegion gameRegion);

}

