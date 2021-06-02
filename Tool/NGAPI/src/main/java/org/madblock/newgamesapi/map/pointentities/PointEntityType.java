package org.madblock.newgamesapi.map.pointentities;

import cn.nukkit.level.Level;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.map.types.PointEntity;

import java.util.HashMap;
import java.util.function.Consumer;

public class PointEntityType {

    protected PointEntityTypeManager manager;
    protected GameHandler gameHandler;
    protected String id;

    private HashMap<String, Consumer<PointEntityCallData>> functions;
    private HashMap<PointEntity, HashMap<String, String>> retainedData;

    public PointEntityType(String id, GameHandler gameHandler) {
        this.gameHandler = gameHandler;
        this.id = id.toLowerCase();
        this.functions = new HashMap<>();
        this.retainedData = new HashMap<>();
    }

    public final boolean executeFunction(String functionID, PointEntity entity, Level level, HashMap<String, String> params){
        String funcID = functionID.toLowerCase();
        if(functions.containsKey(funcID)){
            Consumer<PointEntityCallData> func = functions.get(funcID);
            if(func != null){
                if(!retainedData.containsKey(entity)){
                    retainedData.put(entity, new HashMap<>());
                }
                PointEntityCallData data = new PointEntityCallData(entity, level, params, retainedData.get(entity));
                func.accept(data);
                retainedData.put(entity, data.getRetainedData());
                return true;
            }
        }
        return false;
    }

    public final boolean executeFunctionForAll(String functionID, HashMap<String, String> params){
        for(PointEntity entity: manager.getTypeLookup().get(id)){
            if(!executeFunction(functionID, entity, manager.getLevelLookup().get(entity), params)) return false;
        }
        return true;
    }

    protected void addFunction(String functionID, Consumer<PointEntityCallData> function){
        functions.put(functionID.toLowerCase(), function);
    }

    protected final void setParentManager(PointEntityTypeManager manager){
        this.manager = manager;
    }

    public void onRegister(){ }
    public void onUnregister(){ }
    public void onAddPointEntity(PointEntity entity){ executeFunction("init_entity", entity, manager.getLevelLookup().get(entity), new HashMap<>()); }
    public void onRemovePointEntity(PointEntity entity){ executeFunction("remove_entity", entity, manager.getLevelLookup().get(entity), new HashMap<>()); }
    public void onTick(int tick){
        HashMap<String, String> params = new HashMap<>();
        params.put("tick", String.valueOf(tick));
        executeFunctionForAll("tick_entity", params);
    }

    public PointEntityTypeManager getParentManager() { return manager; }
    public GameHandler getGameHandler() { return gameHandler; }
    public String getId() { return id; }
    public HashMap<String, Consumer<PointEntityCallData>> getFunctions() { return new HashMap<>(functions); }
    public HashMap<PointEntity, HashMap<String, String>> getRetainedData() { return retainedData; }
}
