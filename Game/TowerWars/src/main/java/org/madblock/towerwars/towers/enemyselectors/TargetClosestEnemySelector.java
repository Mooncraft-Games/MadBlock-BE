package org.madblock.towerwars.towers.enemyselectors;

import org.madblock.towerwars.enemies.enemy.Enemy;
import org.madblock.towerwars.towers.tower.Tower;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class TargetClosestEnemySelector implements EnemySelector {

    @Override
    public Set<Enemy> getTargets(Tower tower, Set<Enemy> enemies) {
        Stream<Enemy> enemiesSorted = enemies.stream()
                .filter(enemy -> getDistanceToTower(tower, enemy) < tower.getProperties().getBlockRange())
                .sorted((enemyA, enemyB) -> -Double.compare(getDistanceToTower(tower, enemyA), getDistanceToTower(tower, enemyB)));
        Optional<Enemy> target = enemiesSorted.findAny();

        return target.map(Collections::singleton)
                .orElse(Collections.emptySet());
    }

    @Override
    public String getTargetDescription() {
        return "closest enemy";
    }

    private static double getDistanceToTower(Tower tower, Enemy enemy) {
        return enemy.getEntity().distance(tower.getEntity());
    }

}
