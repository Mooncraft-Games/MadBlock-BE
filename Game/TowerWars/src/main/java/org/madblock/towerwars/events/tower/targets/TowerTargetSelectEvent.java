package org.madblock.towerwars.events.tower.targets;

import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.enemies.enemy.Enemy;
import org.madblock.towerwars.events.tower.TowerEvent;
import org.madblock.towerwars.towers.tower.Tower;

public class TowerTargetSelectEvent extends TowerEvent {

    private Enemy target;

    public TowerTargetSelectEvent(TowerWarsBehavior behavior, Tower tower, Enemy target) {
        super(behavior, tower);
        this.target = target;
    }

    public Enemy getTarget() {
        return this.target;
    }

}
