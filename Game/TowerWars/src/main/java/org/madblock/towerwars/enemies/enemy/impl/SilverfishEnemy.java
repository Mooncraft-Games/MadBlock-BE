package org.madblock.towerwars.enemies.enemy.impl;

import cn.nukkit.entity.Entity;
import cn.nukkit.level.Position;
import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.enemies.enemy.Enemy;
import org.madblock.towerwars.enemies.enemy.EnemyProperties;
import org.madblock.towerwars.utils.GameRegion;

public class SilverfishEnemy extends Enemy {

    public SilverfishEnemy(EnemyProperties properties, TowerWarsBehavior behavior, GameRegion gameRegion) {
        super(properties, behavior, gameRegion);
    }

    @Override
    protected Entity createEntity(Position position) {
        Entity silverFish = Entity.createEntity("Silverfish", position);
        return silverFish;
    }

}
