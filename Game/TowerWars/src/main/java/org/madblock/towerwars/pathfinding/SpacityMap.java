package org.madblock.towerwars.pathfinding;

import cn.nukkit.block.Block;
import cn.nukkit.level.ChunkManager;
import org.madblock.newgamesapi.map.types.MapRegion;

/**
 * The spacity map is used to ensure that pathfinding does not strictly follow the shortest route,
 * but rather a route tries to keep the path in it's own lane
 *
 * It maps the amount of space from a wall to identify a route
 */
public class SpacityMap {

    public static final int WALL_SPACITY = 1;   // What spacity should a wall be assigned

    private final int[][] map;


    public SpacityMap(ChunkManager chunkManager, MapRegion boundaries) {
        int width = boundaries.getPosGreater().getX() - boundaries.getPosLesser().getX();
        int height = boundaries.getPosGreater().getZ() - boundaries.getPosLesser().getZ();
        this.map = new int[height + 1][width + 1];

        // Fill in all the impassible blocks and mark available blocks
        for (int z = 0; z < this.map.length; z++) {
            for (int x = 0; x < this.map[0].length; x++) {
                if (chunkManager.getBlockIdAt(boundaries.getPosLesser().getX() + x, boundaries.getPosLesser().getY(), boundaries.getPosLesser().getZ() + z) != Block.AIR) {
                    this.map[z][x] = WALL_SPACITY;
                } else if (x == 0 || x == this.map[0].length - 1 || z == 0 || z == this.map.length - 1) {
                    this.map[z][x] = WALL_SPACITY + 1;
                }
            }
        }

        // Start creating spacity grid data
        boolean incomplete = true;
        int triggerSpacity = WALL_SPACITY; // what spacity value should we change the adjacent neighbours on?
        while (incomplete) {
            incomplete = false; // This is kept false if no changes are made to the 2d array.
            for (int z = 0; z < this.map.length; z++) {
                for (int x = 0; x < this.map[0].length; x++) {
                    if (this.map[z][x] == triggerSpacity) {

                        // Up
                        if (z > 0 && this.map[z - 1][x] == 0) {
                            this.map[z - 1][x] = triggerSpacity + 1;
                            incomplete = true;
                        }

                        // Down
                        if (z + 1 < this.map.length && this.map[z + 1][x] == 0) {
                            this.map[z + 1][x] = triggerSpacity + 1;
                            incomplete = true;
                        }

                        // Left
                        if (x > 0 && this.map[z][x - 1] == 0) {
                            this.map[z][x - 1] = triggerSpacity + 1;
                            incomplete = true;
                        }

                        // Right
                        if (x + 1 < this.map[0].length && this.map[z][x + 1] == 0) {
                            this.map[z][x + 1] = triggerSpacity + 1;
                            incomplete = true;
                        }

                    }
                }
            }
            triggerSpacity++;
        }
    }

    public int getSpacityAt(int x, int z) {
        return this.map[z][x];
    }

    public int[][] getMap() {
        return this.map;
    }

}
