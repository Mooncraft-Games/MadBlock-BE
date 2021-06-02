package org.madblock.towerwars.towers.events.structure;

import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.towers.events.TowerEvent;
import org.madblock.towerwars.towers.tower.Tower;

public class TowerCreationEvent extends TowerEvent {
    public TowerCreationEvent(TowerWarsBehavior behavior, Tower tower) {
        super(behavior, tower);
    }
}
