package org.madblock.towerwars.pathfinding;

import java.util.Objects;

public class Vector2 {

    private int x;
    private int z;

    public Vector2(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int getX() {
        return this.x;
    }

    public int getZ() {
        return this.z;
    }

    public Vector2 add(int x, int z) {
        return new Vector2(this.x + x, this.z + z);
    }

    public Vector2 add(Vector2 vector2) {
        return this.add(vector2.getX(), vector2.getZ());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.x, this.z);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vector2) {
            return ((Vector2)obj).getX() == this.getX() && ((Vector2)obj).getZ() == this.getZ();
        } else {
            return false;
        }
    }
}
