package org.madblock.towerwars.events.tower.structure;

import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.events.tower.TowerEvent;
import org.madblock.towerwars.towers.tower.Tower;

public class TowerDestructionEvent extends TowerEvent {
    public TowerDestructionEvent(TowerWarsBehavior behavior, Tower tower) {
        super(behavior, tower);
    }
}
