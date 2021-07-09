package org.madblock.towerwars.utils;

import java.util.Objects;

public class Vector2 {

    private double x;
    private double z;

    public Vector2(double x, double z) {
        this.x = x;
        this.z = z;
    }

    public double getX() {
        return this.x;
    }

    public double getZ() {
        return this.z;
    }

    public Vector2 add(double x, double z) {
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