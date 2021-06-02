package org.madblock.newgamesapi.map.types;

import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.math.BlockVector3;
import cn.nukkit.math.Vector2;
import cn.nukkit.math.Vector3;

public class RotatablePosition {

    protected transient String id;
    protected transient boolean isEditingAllowed;

    protected double x;
    protected double y;
    protected double z;

    protected double pitch;
    protected double yaw;

    protected boolean is_accurate_position;

    public RotatablePosition(String id, boolean isEditingAllowed,  Location location) { this(id, isEditingAllowed, location.getX(), location.getY(), location.getZ(), location.getPitch(), location.getYaw(), false); }
    public RotatablePosition(String id, boolean isEditingAllowed,  BlockVector3 blockVector, double pitch, double yaw) { this(id, isEditingAllowed, blockVector.getX(), blockVector.getY(), blockVector.getZ(), pitch, yaw, false); }
    public RotatablePosition(String id, boolean isEditingAllowed, double x, double y, double z, double pitch, double yaw, boolean is_accurate_position){
        this.id = id == null ? "NULL" : id;
        this.isEditingAllowed = isEditingAllowed;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
        this.is_accurate_position = is_accurate_position;
    }

    public Vector3 positionToVector3(){ return new Vector3(x, y, z); }
    public Vector2 rotationToVector2(){ return new Vector2(pitch, yaw); }
    public Location toLocation(Level level){ return new Location(x, y, z, yaw, pitch, level); }

    public RotatablePosition subtractPosition(double value){ return this.subtractPosition(value, value, value); }
    public RotatablePosition subtractPosition(RotatablePosition position){ return this.subtractPosition(position.x, position.y, position.z); }
    public RotatablePosition subtractPosition(double x, double y, double z){ return this.addPosition(-x, -y, -z); }

    public RotatablePosition addPosition(double value){ return this.addPosition(value, value, value); }
    public RotatablePosition addPosition(RotatablePosition position){ return this.addPosition(position.x, position.y, position.z); }
    public RotatablePosition addPosition(double x, double y, double z){
        return new RotatablePosition(this.id, this.isEditingAllowed, this.x + x, this.y + y, this.z + z, pitch, yaw, is_accurate_position);
    }

    public RotatablePosition multiplyPosition(double value){ return this.multiplyPosition(value, value, value); }
    public RotatablePosition multiplyPosition(RotatablePosition position){ return this.multiplyPosition(position.x, position.y, position.z); }
    public RotatablePosition multiplyPosition(double x, double y, double z){
        return new RotatablePosition(this.id, this.isEditingAllowed, this.x * x, this.y * y, this.z * z, pitch, yaw, is_accurate_position);
    }

    public RotatablePosition dividePosition(double value){ return this.dividePosition(value, value, value); }
    public RotatablePosition dividePosition(RotatablePosition position){ return this.dividePosition(position.x, position.y, position.z); }
    public RotatablePosition dividePosition(double x, double y, double z){
        return new RotatablePosition(this.id, this.isEditingAllowed, this.x / x, this.y / y, this.z / z, pitch, yaw, is_accurate_position);
    }

    /**
     * Duplicates the object with the option of enabling editing.
     * @param isEditingAllowed true editing will be allowed on the returned object.
     * @return a new RotatablePosition object with the same properties.
     */
    public RotatablePosition copyRotatablePosition(boolean isEditingAllowed){
        return new RotatablePosition(this.id, isEditingAllowed, this.x, this.y, this.z, this.pitch, this.yaw, this.is_accurate_position);
    }

    public String getId() { return id; }
    public boolean isDirectEditingEnabled() { return isEditingAllowed; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getPitch() { return pitch; }
    public double getYaw() { return yaw; }
    public boolean isAccuratePosition() { return is_accurate_position; }


    public boolean setId(String id) {
        if(isEditingAllowed) {
            this.id = id;
            return true;
        }
        return false;
    }

    public boolean setX(double x) {
        if(isEditingAllowed) {
            this.x = x;
            return true;
        }
        return false;
    }

    public boolean setY(double y) {
        if(isEditingAllowed) {
            this.y = y;
            return true;
        }
        return false;
    }

    public boolean setZ(double z) {
        if(isEditingAllowed) {
           this.z = z;
            return true;
        }
        return false;
    }

    public boolean setPitch(double pitch) {
        if(isEditingAllowed) {
            this.pitch = pitch;
            return true;
        }
        return false;
    }

    public boolean setYaw(double yaw) {
        if(isEditingAllowed) {
            this.yaw = yaw;
            return true;
        }
        return false;
    }

    public boolean setIsAccuratePosition(boolean is_accurate_position) {
        if(isEditingAllowed) {
            this.is_accurate_position = is_accurate_position;
            return true;
        }
        return false;
    }
}
