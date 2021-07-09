package org.madblock.towerwars.utils;

import java.util.Objects;

public class Vector2i {

    private int x;
    private int z;

    public Vector2i(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int getX() {
        return this.x;
    }

    public int getZ() {
        return this.z;
    }

    public Vector2i add(int x, int z) {
        return new Vector2i(this.x + x, this.z + z);
    }

    public Vector2i add(Vector2i vector2) {
        return this.add(vector2.getX(), vector2.getZ());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.x, this.z);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Vector2i) {
            return ((Vector2i)obj).getX() == this.getX() && ((Vector2i)obj).getZ() == this.getZ();
        } else {
            return false;
        }
    }

}
