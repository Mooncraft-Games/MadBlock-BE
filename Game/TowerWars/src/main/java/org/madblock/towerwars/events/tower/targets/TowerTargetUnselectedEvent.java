package org.madblock.towerwars.events.tower.targets;

import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.enemies.enemy.Enemy;
import org.madblock.towerwars.events.tower.TowerEvent;
import org.madblock.towerwars.towers.tower.Tower;

public class TowerTargetUnselectedEvent extends TowerEvent {

    private final Enemy enemy;


    public TowerTargetUnselectedEvent(TowerWarsBehavior behavior, Tower tower, Enemy enemy) {
        super(behavior, tower);
        this.enemy = enemy;
    }

    public Enemy getEnemy() {
        return this.enemy;
    }

}
