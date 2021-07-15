package org.madblock.towerwars.enemies;

import org.madblock.towerwars.enemies.types.EnemyType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EnemyRegistry {

    private final Map<String, EnemyType> enemyTypes = new HashMap<>();

    public EnemyType getEnemyType(String enemyTypeId) {
        return this.enemyTypes.get(enemyTypeId);
    }

    public Set<EnemyType> getTypes() {
        return new HashSet<>(this.enemyTypes.values());
    }

    public void register(EnemyType type) {
        this.enemyTypes.put(type.getId(), type);
    }

}
