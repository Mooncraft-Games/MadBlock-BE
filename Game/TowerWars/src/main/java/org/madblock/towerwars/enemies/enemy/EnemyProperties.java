package org.madblock.towerwars.enemies.enemy;

/**
 * Relevant data to a specific enemy type
 * (e.g. restock ticks, initialStock, health, speed, etc)
 */
/**
 * Relevant data to a specific enemy type
 * (e.g. restock ticks, initialStock, health, speed, etc)
 */
public class EnemyProperties {

    private final int restockTicks;
    private final int initialStock;
    private final int health;
    private final int livesCost;
    private final double movementSpeedPerTick;

    public EnemyProperties(int restockTicks, int initialStock, int health, int livesCost, double movementSpeedPerTick) {
        this.restockTicks = restockTicks;
        this.initialStock = initialStock;
        this.health = health;
        this.livesCost = livesCost;
        this.movementSpeedPerTick = movementSpeedPerTick;
    }

    public int getRestockTicks() {
        return this.restockTicks;
    }

    public int getInitialStock() {
        return this.initialStock;
    }

    public int getHealth() {
        return this.health;
    }

    public int getLivesCost() {
        return this.livesCost;
    }

    public double getMovementPerTick() {
        return this.movementSpeedPerTick;
    }

    public static class Builder {

        private int restockTicks;
        private int initialStock;
        private int health;
        private int livesCost;
        private double movementSpeedPerTick;

        public Builder setRestockTicks(int restockTicks) {
            this.restockTicks = restockTicks;
            return this;
        }

        public Builder setInitialStock(int initialStock) {
            this.initialStock = initialStock;
            return this;
        }

        public Builder setHealth(int health) {
            this.health = health;
            return this;
        }

        public Builder setLivesCost(int cost) {
            this.livesCost = cost;
            return this;
        }

        /**
         * Maximum value has a hard cap of 1 block.
         * @param movementSpeedPerTick
         * @return
         */
        public Builder setMovementSpeedPerTick(double movementSpeedPerTick) {
            if (movementSpeedPerTick > 1d) {
                throw new IllegalArgumentException("movement speed per tick cannot be greater than 1 block.");
            }
            this.movementSpeedPerTick = movementSpeedPerTick;
            return this;
        }

        public EnemyProperties build() {
            return new EnemyProperties(this.restockTicks, this.initialStock, this.health, this.livesCost, this.movementSpeedPerTick);
        }

    }

}
