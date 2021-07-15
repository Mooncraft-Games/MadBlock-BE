package org.madblock.towerwars.events.enemy.states;

import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.enemies.enemy.Enemy;
import org.madblock.towerwars.events.enemy.EnemyEvent;

public class EnemySpawnEvent extends EnemyEvent {

    public EnemySpawnEvent(TowerWarsBehavior behavior, Enemy enemy) {
        super(behavior, enemy);
    }

}
