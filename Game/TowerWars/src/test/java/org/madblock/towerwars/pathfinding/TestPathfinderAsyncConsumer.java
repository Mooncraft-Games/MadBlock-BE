package org.madblock.towerwars.pathfinding;

import cn.nukkit.block.Block;
import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.math.BlockVector3;
import cn.nukkit.math.Vector3;
import org.junit.jupiter.api.Test;
import org.madblock.newgamesapi.map.types.MapRegion;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class TestPathfinderAsyncConsumer {

    @Test
    public void shouldPathfindToAnswer() {

        // 1 = wall, 0 = path
        int[][] gameMap = {
                { 0, 0, 1, 1, 1, 1, 1, 1, 1, 1 },
                { 0, 0, 1, 0, 0, 1, 1, 1, 1, 1 },
                { 0, 0, 0, 0, 0, 1, 1, 1, 1, 1 },
                { 1, 1, 0, 1, 0, 1, 0, 0, 0, 0 },
                { 1, 1, 0, 1, 0, 1, 0, 1, 1, 1 },
                { 1, 1, 0, 1, 0, 0, 0, 1, 1, 1 },
                { 1, 1, 0, 1, 1, 1, 0, 1, 1, 1 },
                { 1, 0, 0, 1, 1, 1, 0, 0, 0, 0 },
                { 1, 0, 1, 1, 0, 1, 0, 0, 0, 0 },
                { 1, 0, 0, 0, 0, 1, 1, 1, 0, 0 }
        };

        MapRegion endGoalRegion = new MapRegion(
                null,
                new BlockVector3(8, 0, 7),
                new BlockVector3(9, 0, 9),
                new String[]{},
                true
        );
        MapRegion boundaries = new MapRegion(
                null,
                new BlockVector3(0, 0, 0),
                new BlockVector3(9, 9, 9),
                new String[]{},
                true
        );

        ChunkManager mockChunkManager = new MockChunkManager(gameMap);
        PathfinderAsyncConsumer pathfinder = new PathfinderAsyncConsumer(
                new PathfinderAsyncConsumer.Settings.Builder()
                        .setLevel(mockChunkManager)
                        .setInitialPosition(new Vector3(0, 0, 0))
                        .setBoundaries(boundaries)
                        .setEndGoalRegion(endGoalRegion)
                        .setSpacityMap(new SpacityMap(mockChunkManager, boundaries))
                        .build()
        );
        List<Vector2> path = pathfinder.get();
        for (Vector2 step : path) {
            if (mockChunkManager.getBlockIdAt((int)step.getX(), 0, (int)step.getZ()) != Block.AIR) {
                fail("Attempted to walk through wall while pathfinding.");
            }
        }

        assertTrue(path.size() > 0, "There should be a path found.");
        Vector2 endPosition = path.get(path.size() - 1);
        assertTrue(endGoalRegion.isWithinThisRegion(new Vector3(endPosition.getX(), endGoalRegion.getPosLesser().getY(), endPosition.getZ())), "Should be in end goal.");
    }

    @Test
    public void shouldNotAlwaysHugCorners() {

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


        MapRegion endGoalRegion = new MapRegion(
                null,
                new BlockVector3(6, 0, 7),
                new BlockVector3(8, 0, 7),
                new String[]{},
                true
        );
        MapRegion boundaries = new MapRegion(
                null,
                new BlockVector3(0, 0, 0),
                new BlockVector3( 9, 0, 7),
                new String[]{},
                true
        );
        Vector3 initialPosition = new Vector3(0, 0, 2);
        ChunkManager mockChunkManager = new MockChunkManager(gameMap);
        SpacityMap spacityMap = new SpacityMap(mockChunkManager, boundaries);

        PathfinderAsyncConsumer pathfinder = new PathfinderAsyncConsumer(
                new PathfinderAsyncConsumer.Settings.Builder()
                        .setLevel(mockChunkManager)
                        .setInitialPosition(initialPosition)
                        .setBoundaries(boundaries)
                        .setEndGoalRegion(endGoalRegion)
                        .setSpacityMap(spacityMap)
                        .build()
        );

        List<Vector2> path = pathfinder.get();

        if (path.size() == 0) {
            throw new AssertionError("Spacity test refused to return path. Is pathfinder properly configured?");
        }

        // We should only be travelling on our spacity for this game map.
        int targetSpacity = spacityMap.getSpacityAt(initialPosition.getFloorX(), initialPosition.getFloorZ());
        int incorrectSpacities = 0;
        for (Vector2 step : path) {
            if (spacityMap.getSpacityAt((int)step.getX(), (int)step.getZ()) != targetSpacity) {
                incorrectSpacities++;
                if (incorrectSpacities > path.size() / 4) {
                    fail("Spacity test failed. Majority of path returned spacity of " + spacityMap.getSpacityAt((int)step.getX(), (int)step.getZ()) + " instead of " + targetSpacity);
                }
            }
        }

    }


    private static class MockChunkManager implements ChunkManager {

        private int[][] gameMap;

        public MockChunkManager(int[][] gameMap) {
            this.gameMap = gameMap;
        }

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
    }

}