package org.madblock.towerwars.towers.events.targets;

import cn.nukkit.level.Position;
import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.enemies.enemy.Enemy;
import org.madblock.towerwars.towers.events.TowerEvent;
import org.madblock.towerwars.towers.tower.Tower;

public class TowerTargetMoveEvent extends TowerEvent {

    private final Enemy target;
    private final Position from;
    private final Position to;

    public TowerTargetMoveEvent(TowerWarsBehavior behavior, Tower tower, Enemy target, Position from, Position to) {
        super(behavior, tower);
        this.target = target;
        this.from = from;
        this.to = to;
    }

    public Position getFrom() {
        return this.from;
    }

    public Position getTo() {
        return this.to;
    }

    public Enemy getTarget() {
        return this.target;
    }

}
