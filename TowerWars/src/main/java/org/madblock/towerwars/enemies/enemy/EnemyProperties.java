package org.madblock.towerwars.enemies.enemy;

/**
 * Relevant data to a specific enemy type
 * (e.g. restock ticks, initialStock, health, speed, etc)
 */
public class EnemyProperties {

    private final int restockTicks;
    private final int initialStock;

    public EnemyProperties(int restockTicks, int initialStock) {
        this.restockTicks = restockTicks;
        this.initialStock = initialStock;
    }

    public int getRestockTicks() {
        return this.restockTicks;
    }

    public int getInitialStock() {
        return this.initialStock;
    }

    public static class Builder {

        private int restockTicks;
        private int initialStock;

        public Builder setRestockTicks(int restockTicks) {
            this.restockTicks = restockTicks;
            return this;
        }

        public Builder setInitialStock(int initialStock) {
            this.initialStock = initialStock;
            return this;
        }

        public EnemyProperties build() {
            return new EnemyProperties(this.restockTicks, this.initialStock);
        }

    }

}
