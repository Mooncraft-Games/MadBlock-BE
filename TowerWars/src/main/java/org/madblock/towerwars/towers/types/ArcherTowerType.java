package org.madblock.towerwars.towers.types;

import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.towers.enemyselectors.TargetClosestEnemySelector;
import org.madblock.towerwars.towers.tower.TowerProperties;
import org.madblock.towerwars.towers.tower.impl.ArcherTower;
import org.madblock.towerwars.towers.tower.Tower;
import org.madblock.towerwars.utils.GameRegion;

public class ArcherTowerType extends TowerType {

    public static String ID = "ARCHER_TOWER";

    public ArcherTowerType(TowerWarsBehavior behavior) {
        super(behavior);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "Archer Tower";
    }

    @Override
    public String getDescription() {
        return "Pew pew! These archers are the best of the best!";
    }

    @Override
    public TowerProperties getTowerProperties() {
        return new TowerProperties.Builder()
                .setAttackInterval(20)
                .setBlockRange(10)
                .setDamage(100)
                .setEnemySelector(new TargetClosestEnemySelector())
                .build();
    }

    @Override
    public int getCost() {
        return 0;
    }

    @Override
    public Tower create(GameRegion gameRegion) {
        return new ArcherTower(this.getTowerProperties(), this.behavior, gameRegion);
    }

}
