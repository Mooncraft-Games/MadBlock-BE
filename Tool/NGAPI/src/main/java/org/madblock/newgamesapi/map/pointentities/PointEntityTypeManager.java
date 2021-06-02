package org.madblock.newgamesapi.map.pointentities;

import cn.nukkit.level.Level;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.map.types.PointEntity;

import java.util.ArrayList;
import java.util.HashMap;

public class PointEntityTypeManager {

    protected GameHandler gameHandler;

    protected HashMap<String, PointEntityType> registeredTypes;
    protected HashMap<String, ArrayList<PointEntity>> typeLookup;
    protected HashMap<String, PointEntity> globalLookup;
    protected HashMap<PointEntity, Level> levelLookup;

    public PointEntityTypeManager(GameHandler gameHandler) {
        this.gameHandler = gameHandler;
        this.registeredTypes = new HashMap<>();
        this.typeLookup = new HashMap<>();
        this.globalLookup = new HashMap<>();
        this.levelLookup = new HashMap<>();
    }

    public void registerPointEntityType(PointEntityType type){
        // Create a new list if none exists, otherwise just steal em.
        type.setParentManager(this);
        if(!typeLookup.containsKey(type.getId())){
            typeLookup.put(type.getId(), new ArrayList<>());
        } else {
            for (PointEntity entity : new ArrayList<>(typeLookup.get(type.getId()))) { type.onRemovePointEntity(entity); }
        }

        if(registeredTypes.containsKey(type.getId())) {
            registeredTypes.get(type.getId()).onUnregister();
        }
        registeredTypes.put(type.getId(), type);
        type.onRegister();
        for (PointEntity entity : new ArrayList<>(typeLookup.get(type.getId()))) { type.onAddPointEntity(entity); }
    }

    public void addPointEntity(PointEntity entity, Level level) {
        if(!globalLookup.containsKey(entity.getId())) {
            levelLookup.put(entity, level);
            globalLookup.put(entity.getId(), entity);

            if(!typeLookup.containsKey(entity.getType())) typeLookup.put(entity.getType(), new ArrayList<>());
            typeLookup.get(entity.getType()).add(entity);

            if(registeredTypes.containsKey(entity.getType())) registeredTypes.get(entity.getType()).onAddPointEntity(entity);
        }
    }

    public void removePointEntity(PointEntity entity) {
        if(globalLookup.containsKey(entity.getId())) {
            levelLookup.remove(entity);
            globalLookup.remove(entity.getId());

            if(typeLookup.containsKey(entity.getType())) typeLookup.get(entity.getType()).remove(entity);

            if(registeredTypes.containsKey(entity.getType())) registeredTypes.get(entity.getType()).onRemovePointEntity(entity);
        }
    }

    public HashMap<String, PointEntityType> getRegisteredTypes() { return registeredTypes; }
    public HashMap<String, ArrayList<PointEntity>> getTypeLookup() { return typeLookup; }
    public HashMap<String, PointEntity> getGlobalLookup() { return globalLookup; }
    public HashMap<PointEntity, Level> getLevelLookup() { return levelLookup; }
}
