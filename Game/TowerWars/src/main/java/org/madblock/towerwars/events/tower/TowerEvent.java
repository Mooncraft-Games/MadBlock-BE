package org.madblock.towerwars.events.tower;

import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.towers.tower.Tower;
import org.madblock.towerwars.events.GameEvent;

public abstract class TowerEvent extends GameEvent {

    private final Tower tower;

    public TowerEvent(TowerWarsBehavior behavior, Tower tower) {
        super(behavior);
        this.tower = tower;
    }

    public Tower getTower() {
        return this.tower;
    }

}
