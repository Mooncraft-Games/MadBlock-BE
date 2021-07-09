package org.madblock.towerwars.events.enemy.states;

import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.enemies.enemy.Enemy;
import org.madblock.towerwars.events.enemy.EnemyEvent;
import org.madblock.towerwars.utils.Vector2;

public class EnemyMoveEvent extends EnemyEvent {

    private Vector2 movementVector;

    public EnemyMoveEvent(TowerWarsBehavior behavior, Enemy enemy, Vector2 movementVector) {
        super(behavior, enemy);
        this.movementVector = movementVector;
    }

    public Vector2 getMovementVector() {
        return this.movementVector;
    }

    public void setMovementVector(Vector2 movementVector) {
        this.movementVector = movementVector;
    }

}
