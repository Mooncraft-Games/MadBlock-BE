package org.madblock.towerwars.pathfinding;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
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

    private final ChunkManager level;

    public Pathfinder(ChunkManager level) {
        this.level = level;
        TowerWarsPlugin.get().getServer().getPluginManager().registerEvents(this, TowerWarsPlugin.get());
    }

    /**
     * The y level is constant and is retrieved from the currentPosition
     * @param gameRegion Game region that contains the play area and the end goal
     * @param currentPosition Where we are currently
     * @return the positions to goto starting from currentPosition
     */
    public CompletableFuture<List<Vector2>> solve(GameRegion gameRegion, Vector3 currentPosition) {
        // Make sure the chunks stay loaded throughout our pathfinding
        Set<FullChunk> loadedChunks = this.loadChunks(gameRegion.getPlayArea());
        this.usedChunks.addAll(loadedChunks);

        return CompletableFuture
                .supplyAsync(() -> new SpacityMap(this.level, gameRegion.getPlayArea()))
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

}
