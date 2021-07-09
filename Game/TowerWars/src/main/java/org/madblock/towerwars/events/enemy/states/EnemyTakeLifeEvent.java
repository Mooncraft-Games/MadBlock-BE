package org.madblock.towerwars.events.enemy.states;

import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.enemies.enemy.Enemy;
import org.madblock.towerwars.events.enemy.EnemyEvent;

/**
 * Called when a enemy makes it to the end
 */
public class EnemyTakeLifeEvent extends EnemyEvent {

    private int lives;

    public EnemyTakeLifeEvent(TowerWarsBehavior behavior, Enemy enemy, int lives) {
        super(behavior, enemy);
        this.lives = lives;
    }

    public int getLivesCost() {
        return this.lives;
    }

    /**
     * How many lives will this event take from the player
     * @param cost
     */
    public void setLivesCost(int cost) {
        this.lives = cost;
    }

}
