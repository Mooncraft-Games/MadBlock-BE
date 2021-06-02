package org.madblock.newgamesapi.map.types;

import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.exception.MapIDParseException;

import java.util.HashMap;
import java.util.Map;

public class PointEntity extends RotatablePosition {

    protected String type;
    protected HashMap<String, String> string_properties;
    protected HashMap<String, Integer> integer_properties;
    protected HashMap<String, Float> float_properties;
    protected HashMap<String, Boolean> boolean_properties;

    public PointEntity(String id, String type, double x, double y, double z, double pitch, double yaw, boolean is_position_accurate) {
        super(id, false, x, y, z, pitch, yaw, is_position_accurate);
        this.type = type.toLowerCase();
        this.string_properties = new HashMap<>();
        this.integer_properties = new HashMap<>();
        this.float_properties = new HashMap<>();
        this.boolean_properties = new HashMap<>();

    }

    public String verifyIntegrityFromJson(String uid) throws MapIDParseException {
        if(uid == null || uid.trim().equals("")) throw new MapIDParseException("A point entity has a missing uid. Skipping.");
        if(this.type == null || this.type.trim().equals("")) throw new MapIDParseException(String.format("Type for point entity with id [%s] is empty. This is not allowed.", uid));
        if(this.string_properties == null) this.string_properties = new HashMap<>();
        if(this.integer_properties == null) this.integer_properties = new HashMap<>();
        if(this.float_properties == null) this.float_properties = new HashMap<>();
        if(this.boolean_properties == null) this.boolean_properties = new HashMap<>();

        this.id = uid.toLowerCase().trim();
        this.type = type.toLowerCase().trim();
        this.type = type.toLowerCase().trim();

        this.string_properties = verifyIDMap(string_properties);
        this.integer_properties = verifyIDMap(integer_properties);
        this.float_properties = verifyIDMap(float_properties);
        this.boolean_properties = verifyIDMap(boolean_properties);

        this.isEditingAllowed = false;
        return this.id;
    }

    protected <V> HashMap<String, V> verifyIDMap(HashMap<String, V> hashMap){
        HashMap<String, V> returnMap = new HashMap<>();
        for(Map.Entry<String, V> entry : new HashMap<>(hashMap).entrySet()){
            returnMap.remove(entry.getKey());
            if(entry.getKey() == null || entry.getKey().trim().equals("")){
                NewGamesAPI1.getPlgLogger().warning(String.format("String Property key in PointEntity:%s is null. Skipping.", id));
                continue;
            }
            if(entry.getValue() != null) {
                returnMap.put(entry.getKey().toLowerCase(), entry.getValue());
            } else {
                NewGamesAPI1.getPlgLogger().warning(String.format("String Property [%s@PointEntity:%s] is null. Skipping.", entry.getKey(), id));
            }
        }
        return returnMap;
    }

    public String getType() { return type; }
    public HashMap<String, String> getStringProperties() { return isEditingAllowed ? string_properties : new HashMap<>(string_properties); }
    public HashMap<String, Integer> getIntegerProperties() { return isEditingAllowed ? integer_properties : new HashMap<>(integer_properties); }
    public HashMap<String, Float> getFloatProperties() { return isEditingAllowed ? float_properties : new HashMap<>(float_properties); }
    public HashMap<String, Boolean> getBooleanProperties() { return isEditingAllowed ? boolean_properties : new HashMap<>(boolean_properties); }
}
