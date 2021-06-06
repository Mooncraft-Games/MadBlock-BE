package org.madblock.towerwars.utils;

import org.madblock.newgamesapi.map.types.MapRegion;

/**
 * Contains the regions that make up a player's space
 */
public class GameRegion {

    private final MapRegion playArea;
    private final MapRegion spawnMonstersArea;
    private final MapRegion endGoalArea;

    public GameRegion(MapRegion playArea, MapRegion spawnMonstersArea, MapRegion endGoalArea) {
        this.playArea = playArea;
        this.spawnMonstersArea = spawnMonstersArea;
        this.endGoalArea = endGoalArea;
    }

    public MapRegion getPlayArea() {
        return this.playArea;
    }

    public MapRegion getSpawnMonstersArea() {
        return this.spawnMonstersArea;
    }

    public MapRegion getEndGoalArea() {
        return this.endGoalArea;
    }
}
