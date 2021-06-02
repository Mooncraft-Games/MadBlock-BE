package org.madblock.newgamesapi.map.functionalregions;

import cn.nukkit.level.Level;
import cn.nukkit.scheduler.TaskHandler;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.map.types.MapRegion;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Consumer;

public class FunctionalRegionManager {

    private GameHandler handler;
    private HashMap<String, FunctionalRegionTag> mapRegions;
    private HashMap<String, TaskHandler> taskHandlers;

    public FunctionalRegionManager(GameHandler handler) {
        this.handler = handler;
        this.mapRegions = new HashMap<>();
        this.taskHandlers = new HashMap<>();
    }

    /**
     * Sets a tag function to only be triggered by manual calls.
     * @param tag the id of the tag to pair the function with.
     * @param function the function to be called when triggered.
     */
    public void setTagFunction(String tag, Consumer<FunctionalRegionCallData> function){
        setTagFunction(tag, function, -1, 0);
    }

    /**
     * Sets a tag function to be triggered at regular intervals
     * @param tag the id of the tag to pair the function with.
     * @param function the function to be called on a loop.
     * @param repeatInterval how many ticks are in-between loops
     */
    public void setTagFunction(String tag, Consumer<FunctionalRegionCallData> function, int repeatInterval){
        setTagFunction(tag, function, repeatInterval, 0);
    }

    /**
     * Sets a tag function to be triggered at regular intervals
     * @param tag the id of the tag to pair the function with.
     * @param function the function to be called on a loop.
     * @param repeatInterval how many ticks are in-between loops.
     * @param intitialDelay how many ticks should pass before the first call.
     */
    public void setTagFunction(String tag, Consumer<FunctionalRegionCallData> function, int repeatInterval, int intitialDelay) {
        String lowerTag = tag.toLowerCase();
        clearTagFunction(tag);
        if (mapRegions.get(lowerTag) == null) {
            mapRegions.put(lowerTag, new FunctionalRegionTag(tag, function));
        } else {
            mapRegions.get(lowerTag).setFunction(function);
        }
        if (repeatInterval > -1){
            taskHandlers.put(lowerTag, handler.getGameScheduler().registerGameTask(mapRegions.get(lowerTag)::runFunction, intitialDelay, repeatInterval));
        }
    }

    public void registerRegion(MapRegion region, Level level){
        for(String tag: region.getTags()){
            FunctionalRegionCallData.ParseResult result = FunctionalRegionCallData.parseTagArgs(tag);
            String lowerTag = result.getTag().toLowerCase();
            if(mapRegions.get(lowerTag) == null){
                mapRegions.put(lowerTag, new FunctionalRegionTag(result.getTag(), null));
            }
            mapRegions.get(lowerTag).addRegion(region, level, result);
        }
    }

    public void unregisterRegion(MapRegion region){
        for(String tag: region.getTags()){
            String lowerTag = tag.toLowerCase();
            if(mapRegions.get(lowerTag) != null){
                mapRegions.get(lowerTag).removeRegion(region);
            }
        }
    }

    public void clearTag(String tag){
        clearTagFunction(tag);
        clearTagRegions(tag);
    }

    public void clearTagRegions(String tag){
        String lowerTag = tag.toLowerCase();
        if(mapRegions.containsKey(lowerTag)){

        }
    }

    public void clearTagFunction(String tag){
        String lowerTag = tag.toLowerCase();
        if(taskHandlers.containsKey(lowerTag)){
            taskHandlers.get(lowerTag).cancel();
            taskHandlers.remove(lowerTag);
        }
        if(mapRegions.containsKey(lowerTag)){
            mapRegions.get(lowerTag).setFunction(null);
        }
    }

    public Optional<FunctionalRegionTag> getRegionFunctionForTag(String tag) {
        return Optional.ofNullable(mapRegions.get(tag.toLowerCase()));
    }
}
