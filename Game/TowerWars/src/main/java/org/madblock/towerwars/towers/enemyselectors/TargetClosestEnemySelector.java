package org.madblock.towerwars.towers.enemyselectors;

import org.madblock.towerwars.enemies.enemy.Enemy;
import org.madblock.towerwars.towers.tower.Tower;

import java.util.HashSet;
import java.util.Set;

public class TargetClosestEnemySelector implements EnemySelector {

    @Override
    public Set<Enemy> getTargets(Tower tower, Set<Enemy> enemies) {
        return new HashSet<>();
    }

    @Override
    public String getTargetDescription() {
        return "closest enemy";
    }

}
