package org.madblock.towerwars.pathfinding;

import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.math.BlockVector3;
import org.junit.jupiter.api.Test;
import org.madblock.newgamesapi.map.types.MapRegion;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class TestPathfinder {

    @Test
    public void shouldConstructProperSpacityMap() {

        // 1 = wall, 0 = path
        int[][] gameMap = {
                { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
                { 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
                { 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
                { 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
                { 1, 1, 1, 1, 1, 1, 0, 0, 0, 1 },
                { 1, 1, 1, 1, 1, 1, 0, 0, 0, 1 },
                { 1, 1, 1, 1, 1, 1, 0, 0, 0, 1 },
                { 1, 1, 1, 1, 1, 1, 0, 0, 0, 1 }
        };

        int[][] spacityMap = {
                { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 },
                { 2, 2, 2, 2, 2, 2, 2, 2, 2, 1 },
                { 2, 3, 3, 3, 3, 3, 3, 3, 2, 1 },
                { 2, 2, 2, 2, 2, 2, 3, 3, 2, 1 },
                { 1, 1, 1, 1, 1, 1, 2, 3, 2, 1 },
                { 1, 1, 1, 1, 1, 1, 2, 3, 2, 1 },
                { 1, 1, 1, 1, 1, 1, 2, 3, 2, 1 },
                { 1, 1, 1, 1, 1, 1, 2, 2, 2, 1 },
        };

        int[][] constructedSpacityMap = Pathfinder.getSpacityMap(new ChunkManager() {
            @Override
            public int getBlockIdAt(int x, int y, int z) {
                return gameMap[z][x];
            }

            @Override
            public void setBlockFullIdAt(int x, int y, int z, int fullId) {

            }

            @Override
            public void setBlockIdAt(int x, int y, int z, int id) {

            }

            @Override
            public void setBlockAt(int x, int y, int z, int id, int data) {

            }

            @Override
            public int getBlockDataAt(int x, int y, int z) {
                return 0;
            }

            @Override
            public void setBlockDataAt(int x, int y, int z, int data) {

            }

            @Override
            public BaseFullChunk getChunk(int chunkX, int chunkZ) {
                return null;
            }

            @Override
            public void setChunk(int chunkX, int chunkZ) {

            }

            @Override
            public void setChunk(int chunkX, int chunkZ, BaseFullChunk chunk) {

            }

            @Override
            public long getSeed() {
                return 0;
            }
        }, new MapRegion(
                null,
                new BlockVector3(0, 0, 0),
                new BlockVector3(gameMap[0].length - 1, 0, gameMap.length - 1),
                new String[]{},
                true
        ));

        for (int rowI = 0; rowI < gameMap.length; rowI++) {
            assertArrayEquals(spacityMap[rowI], constructedSpacityMap[rowI]);
        }

    }



}