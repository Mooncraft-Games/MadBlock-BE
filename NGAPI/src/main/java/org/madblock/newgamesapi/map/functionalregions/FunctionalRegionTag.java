package org.madblock.newgamesapi.map.functionalregions;

import cn.nukkit.level.Level;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.map.types.MapRegion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

public final class FunctionalRegionTag {

    private String tag;
    private Consumer<FunctionalRegionCallData> function;
    private HashMap<MapRegion, ArrayList<FunctionalRegionCallData>> regions;

    public FunctionalRegionTag(String tag, Consumer<FunctionalRegionCallData> function){
        this.tag = tag;
        this.function = function;
        this.regions = new HashMap<>();
    }

    protected void addRegion(MapRegion region, Level level, FunctionalRegionCallData.ParseResult parseResult){
        if(parseResult.getTag().toLowerCase().equals(tag.toLowerCase())){
            if(!regions.containsKey(region)){
                regions.put(region, new ArrayList<>());
            }
            regions.get(region).add(new FunctionalRegionCallData(region, level, parseResult.getTag(), parseResult.getArgs()));
        }
    }
    protected void removeRegion(MapRegion region){ regions.remove(region); }

    public void setFunction(Consumer<FunctionalRegionCallData> function) { this.function = function; }

    public void runFunction(){
        if(function != null) {
            for (ArrayList<FunctionalRegionCallData> regiondata : new HashMap<>(regions).values()) {
                for(FunctionalRegionCallData call: regiondata) {
                    try {
                        function.accept(call);
                    } catch (Exception err) {
                        NewGamesAPI1.getPlgLogger().warning("Error whilst ticking region: " + call.getRegion().toString());
                        err.printStackTrace();
                    }
                }
            }
        }
    }

    public HashMap<MapRegion, ArrayList<FunctionalRegionCallData>> getRegionsCopy() {
        return new HashMap<>(regions);
    }
}
