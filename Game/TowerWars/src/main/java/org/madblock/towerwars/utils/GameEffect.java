package org.madblock.towerwars.utils;

public abstract class GameEffect {

    private int ticksLeft;

    /**
     * Amount of ticks this effect lasts
     * @return ticks
     */
    abstract int getLifespan();

    public GameEffect() {
        this.ticksLeft = this.getLifespan();
    }

    public void tick() {
        this.ticksLeft -= 1;
    }

    public boolean isActive() {
        return this.ticksLeft > 0;
    }

}
