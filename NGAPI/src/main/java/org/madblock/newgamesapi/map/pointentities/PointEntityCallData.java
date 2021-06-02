package org.madblock.newgamesapi.map.pointentities;

import cn.nukkit.level.Level;
import org.madblock.newgamesapi.map.types.PointEntity;

import java.util.HashMap;

public class PointEntityCallData {

    protected PointEntity pointEntity;
    protected Level level;
    protected HashMap<String, String> parameters;
    protected HashMap<String, String> retainedData;

    public PointEntityCallData(PointEntity pointEntity, Level level, HashMap<String, String> parameters, HashMap<String, String> retainedData) {
        this.pointEntity = pointEntity;
        this.level = level;
        this.parameters = parameters;
        this.retainedData = retainedData;
    }

    public PointEntity getPointEntity() { return pointEntity; }
    public Level getLevel() { return level; }
    public HashMap<String, String> getParameters() { return parameters; }
    public HashMap<String, String> getRetainedData() { return retainedData; }
}
