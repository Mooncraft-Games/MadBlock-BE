package org.madblock.towerwars.towers.enemyselectors;

import org.madblock.towerwars.enemies.enemy.Enemy;
import org.madblock.towerwars.towers.tower.Tower;

import java.util.Set;

public interface EnemySelector {

    Set<Enemy> getTargets(Tower tower, Set<Enemy> enemies);

    String getTargetDescription();

}
