package org.madblock.towerwars.events.tower.targets;

import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.enemies.enemy.Enemy;
import org.madblock.towerwars.events.tower.TowerEvent;
import org.madblock.towerwars.towers.tower.Tower;

public class TowerTargetAttackEvent extends TowerEvent {

    private double damage;
    private Enemy target;

    public TowerTargetAttackEvent(TowerWarsBehavior behavior, Tower tower, Enemy target, double damage) {
        super(behavior, tower);
        this.damage = damage;
        this.target = target;
    }

    public double getDamage() {
        return this.damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public Enemy getTarget() {
        return this.target;
    }

    public void setTarget(Enemy target) {
        this.target = target;
    }

}
