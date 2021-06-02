package org.madblock.towerwars.towers.types;

import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.towers.tower.Tower;
import org.madblock.towerwars.towers.tower.TowerProperties;
import org.madblock.towerwars.utils.GameRegion;

public abstract class TowerType {

    protected final TowerWarsBehavior behavior;

    public TowerType(TowerWarsBehavior behavior) {
        this.behavior = behavior;
    }

    public abstract String getId();
    public abstract String getName();
    public abstract String getDescription();
    public abstract TowerProperties getTowerProperties();

    public abstract int getCost();

    public abstract Tower create(GameRegion gameRegion);

}

