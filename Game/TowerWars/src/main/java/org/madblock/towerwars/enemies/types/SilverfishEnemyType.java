package org.madblock.towerwars.enemies.types;

import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.enemies.enemy.Enemy;
import org.madblock.towerwars.enemies.enemy.EnemyProperties;
import org.madblock.towerwars.enemies.enemy.impl.SilverfishEnemy;
import org.madblock.towerwars.utils.GameRegion;

public class SilverfishEnemyType extends EnemyType {

    public static final String ID = "silverfish_enemy_type";

    public SilverfishEnemyType(TowerWarsBehavior behavior) {
        super(behavior);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public EnemyProperties getProperties() {
        return new EnemyProperties.Builder()
                .setMovementSpeedPerTick(1 / 5d).build();
    }

    @Override
    public Enemy create(GameRegion gameRegion) {
        return new SilverfishEnemy(this.getProperties(), this.behavior, gameRegion);
    }

}
