package org.madblock.towerwars.events.enemy.tower;

import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.enemies.enemy.Enemy;
import org.madblock.towerwars.events.enemy.EnemyEvent;

public class EnemyUntargettedEvent extends EnemyEvent {

    public EnemyUntargettedEvent(TowerWarsBehavior behavior, Enemy enemy) {
        super(behavior, enemy);
    }

}
