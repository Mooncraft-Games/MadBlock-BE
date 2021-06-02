package org.madblock.towerwars.towers;

import org.madblock.towerwars.towers.types.TowerType;

import java.util.*;

public class TowerRegistry {

    private final Map<String, TowerType> towerTypes = new HashMap<>();

    public TowerType getType(String towerTypeId) {
        return this.towerTypes.get(towerTypeId);
    }

    public Set<TowerType> getTypes() {
        return new HashSet<>(this.towerTypes.values());
    }

    public void register(TowerType towerType) {
        this.towerTypes.put(towerType.getId(), towerType);
    }

}
