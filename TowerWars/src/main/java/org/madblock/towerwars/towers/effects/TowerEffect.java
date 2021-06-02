package org.madblock.towerwars.towers.effects;

import org.madblock.towerwars.towers.tower.Tower;
import org.madblock.towerwars.utils.GameEffect;

public abstract class TowerEffect extends GameEffect {

    private final Tower tower;

    public TowerEffect(Tower tower) {
        this.tower = tower;
    }

    public Tower getTower() {
        return this.tower;
    }

}
