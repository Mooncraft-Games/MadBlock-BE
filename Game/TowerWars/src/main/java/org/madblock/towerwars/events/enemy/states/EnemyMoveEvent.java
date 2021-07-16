package org.madblock.towerwars.events.enemy.states;

import cn.nukkit.level.Position;
import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.enemies.enemy.Enemy;
import org.madblock.towerwars.events.enemy.EnemyEvent;
import org.madblock.towerwars.utils.Vector2;

public class EnemyMoveEvent extends EnemyEvent {

    private Vector2 movementVector;
    private Position endLocation;

    public EnemyMoveEvent(TowerWarsBehavior behavior, Enemy enemy, Vector2 movementVector) {
        super(behavior, enemy);
        this.movementVector = movementVector;
        this.calculateEndPosition();
    }

    public Vector2 getMovementVector() {
        return this.movementVector;
    }

    public void setMovementVector(Vector2 movementVector) {
        this.movementVector = movementVector;
        this.calculateEndPosition();
    }

    public Position getFrom() {
        return this.getEnemy().getEntity().getPosition();
    }

    public Position getEndLocation() {
        return this.endLocation;
    }

    private void calculateEndPosition() {
        this.endLocation = this.getFrom().add(this.getMovementVector().getX(), 0, this.getMovementVector().getZ());
    }

}
