package org.madblock.towerwars.enemies.effects;

import org.madblock.towerwars.enemies.enemy.Enemy;
import org.madblock.towerwars.utils.GameEffect;

public abstract class EnemyEffect extends GameEffect {

    private final Enemy enemy;

    public EnemyEffect(Enemy enemy) {
        this.enemy = enemy;
    }

    public Enemy getEnemy() {
        return this.enemy;
    }

}
