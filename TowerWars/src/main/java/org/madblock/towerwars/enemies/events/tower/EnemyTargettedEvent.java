package org.madblock.towerwars.enemies.events.tower;

import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.enemies.enemy.Enemy;
import org.madblock.towerwars.enemies.events.EnemyEvent;

public class EnemyTargettedEvent extends EnemyEvent {

    public EnemyTargettedEvent(TowerWarsBehavior behavior, Enemy enemy) {
        super(behavior, enemy);
    }
    
}
