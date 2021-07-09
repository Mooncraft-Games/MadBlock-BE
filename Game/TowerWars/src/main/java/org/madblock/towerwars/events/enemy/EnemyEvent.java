package org.madblock.towerwars.events.enemy;

import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.enemies.enemy.Enemy;
import org.madblock.towerwars.events.GameEvent;

public class EnemyEvent extends GameEvent {

    private final Enemy enemy;

    public EnemyEvent(TowerWarsBehavior behavior, Enemy enemy) {
        super(behavior);
        this.enemy = enemy;
    }

    public Enemy getEnemy() {
        return this.enemy;
    }

}
