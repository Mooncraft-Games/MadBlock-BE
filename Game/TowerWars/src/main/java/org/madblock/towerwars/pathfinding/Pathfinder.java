package org.madblock.towerwars.pathfinding;

import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.level.ChunkUnloadEvent;
import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import org.madblock.newgamesapi.map.types.MapRegion;
import org.madblock.towerwars.TowerWarsPlugin;
import org.madblock.towerwars.utils.GameRegion;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Pathfinder implements Listener {

    // Note: This is a list because it is possible for two pathfinding operations to run at the same time and use the same chunks.
    private final List<FullChunk> usedChunks = Collections.synchronizedList(new ArrayList<>());

    private MapRegion test;
    private final ChunkManager level;

    public Pathfinder(ChunkManager level) {
        this.level = level;
        TowerWarsPlugin.get().getServer().getPluginManager().registerEvents(this, TowerWarsPlugin.get());
    }

    /**
     * The y level is constant and is retrieved from the currentPosition
     * @param gameRegion
     * @param currentPosition
     * @return the positions to goto starting from currentPosition
     */
    public CompletableFuture<List<Vector2>> solve(GameRegion gameRegion, Vector3 currentPosition) {
        this.test = gameRegion.getPlayArea();
        // Make sure the chunks stay loaded throughout our pathfinding
        Set<FullChunk> loadedChunks = this.loadChunks(gameRegion.getPlayArea());
        this.usedChunks.addAll(loadedChunks);

        return CompletableFuture
                .supplyAsync(() -> Pathfinder.getSpacityMap(this.level, gameRegion.getPlayArea()))
                .thenApplyAsync(spacityMap -> new PathfinderAsyncConsumer(
                        new PathfinderAsyncConsumer.Settings.Builder()
                            .setLevel(this.level)
                            .setBoundaries(gameRegion.getPlayArea())
                            .setEndGoalRegion(gameRegion.getEndGoalArea())
                            .setInitialPosition(currentPosition)
                            .setSpacityMap(spacityMap)
                            .build()
                ).get())
                .thenApplyAsync(bestPath -> {
                    // This pathfinding operation is done using these chunks. They can be unloaded.
                    loadedChunks.forEach(this.usedChunks::remove);
                    return bestPath;
                });
    }

    /**
     * Load all chunks within the map region
     * @param playArea region
     * @return all the loaded chunks
     */
    private Set<FullChunk> loadChunks(MapRegion playArea) {
        Set<FullChunk> loadedChunks = new HashSet<>();
        for (int x = playArea.getPosLesser().getX() / 16; x <= Math.ceil(playArea.getPosGreater().getX() / 16d); x++) {
            for (int z = playArea.getPosLesser().getZ() / 16; z <= Math.ceil(playArea.getPosGreater().getZ() / 16d); z++) {
                loadedChunks.add(this.level.getChunk(x, z));
            }
        }
        return loadedChunks;
    }

    public void cleanUp() {
        HandlerList.unregisterAll(this);
        this.usedChunks.forEach(chunk -> {
            try {
                chunk.unload(false);
            } catch (Exception exception) {
                throw new AssertionError(exception);    // This should never print technically as we aren't saving chunks.
            }
        });
        this.usedChunks.clear();
    }

    @EventHandler
    public void onChunkUnloadEvent(ChunkUnloadEvent event) {
        if (this.usedChunks.contains(event.getChunk())) {
            event.setCancelled();   // We're still using this chunk in pathfinding.
        }
    }

    @EventHandler
    public void onDebug(BlockBreakEvent event) {
        event.getPlayer().sendMessage("spacity = " + getSpacityMap(this.level, this.test)[(int)Math.abs(this.test.getPosLesser().getZ() - event.getBlock().getFloorZ())][(int)Math.abs(this.test.getPosLesser().getX() - event.getBlock().getFloorX())]);
        for (int[] row : getSpacityMap(this.level, this.test)) {
            for (int i : row) {
                if (i == 3) {
                    event.getPlayer().sendMessage("spacity other than 1 exists " + i);
                }
            }
        }
    }

    /**
     * Will construct a spacity map based off of the chunkManager.
     * It will use the y level of the lower boundary.
     * @param chunkManager
     * @param boundaries
     * @return
     */

    // TODO: consider edge blocks and cache in actual obj not static
    // TODO: getSpacityMap should have tests too
    public static int[][] getSpacityMap(ChunkManager chunkManager, MapRegion boundaries) {
        int width = boundaries.getPosGreater().getX() - boundaries.getPosLesser().getX();
        int height = boundaries.getPosGreater().getZ() - boundaries.getPosLesser().getZ();
        int[][] spacityMap = new int[height + 1][width + 1];
        boolean incomplete = true;

        // Fill in all the walls
        for (int z = 0; z < spacityMap.length; z++) {
            for (int x = 0; x < spacityMap[0].length; x++) {
                if (chunkManager.getBlockIdAt(boundaries.getPosLesser().getX() + x, boundaries.getPosLesser().getY(), boundaries.getPosLesser().getZ() + z) != Block.AIR) {
                    spacityMap[z][x] = 1;
                }
            }
        }

        // Start creating spacity grid data
        int triggerSpacity = 1; // what spacity value should we change the adjacent neighbours on?
        while (incomplete) {
            incomplete = false; // This is kept false if no changes are made to the 2d array.
            for (int z = 0; z < spacityMap.length; z++) {
                for (int x = 0; x < spacityMap[0].length; x++) {
                    if (spacityMap[z][x] == triggerSpacity) {

                        // Up
                        if (z > 0 && spacityMap[z - 1][x] == 0) {
                            spacityMap[z - 1][x] = triggerSpacity + 1;
                            incomplete = true;
                        }

                        // Down
                        if (z + 1 < spacityMap.length && spacityMap[z + 1][x] == 0) {
                            spacityMap[z + 1][x] = triggerSpacity + 1;
                            incomplete = true;
                        }

                        // Left
                        if (x > 0 && spacityMap[z][x - 1] == 0) {
                            spacityMap[z][x - 1] = triggerSpacity + 1;
                            incomplete = true;
                        }

                        // Right
                        if (x + 1 < spacityMap[0].length && spacityMap[z][x + 1] == 0) {
                            spacityMap[z][x + 1] = triggerSpacity + 1;
                            incomplete = true;
                        }

                    }
                }
            }
            triggerSpacity++;
        }
        return spacityMap;
    }

}
