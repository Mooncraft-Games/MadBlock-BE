package org.madblock.newgamesapi.map.types;

/**
 * A point entity designed to be moved around, it keeps track of it's origin
 * and
 */
public class DynamicPointEntity extends PointEntity {

    protected double originX;
    protected double originY;
    protected double originZ;
    protected double originPitch;
    protected double originYaw;

    public DynamicPointEntity(String id, String type, double x, double y, double z, double pitch, double yaw, boolean is_position_accurate) {
        super(id, type, x, y, z, pitch, yaw, is_position_accurate);
        this.isEditingAllowed = true; // Gonna do a sneaky :)

        if(!is_position_accurate){
            this.x += 0.5;
            this.y += 0.5;
            this.z += 0.5;
            this.is_accurate_position = true;
        }

        this.originX = this.x;
        this.originY = this.y;
        this.originZ = this.z;
        this.originPitch = this.pitch;
        this.originYaw = this.yaw;
    }

    public double getOriginX() { return originX; }
    public double getOriginY() { return originY; }
    public double getOriginZ() { return originZ; }
    public double getOriginPitch() { return originPitch; }
    public double getOriginYaw() { return originYaw; }
}
