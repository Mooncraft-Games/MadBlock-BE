package org.madblock.towerwars.towers.tower;

import org.madblock.towerwars.towers.enemyselectors.EnemySelector;

/**
 * Relevant tower data for an operating tower
 * e.g. damage, the range of a tower, the upgrades it has
 */
public class TowerProperties {

    private final int attackInterval;
    private final double blockRange;
    private final double damage;
    private final EnemySelector enemySelector;
    private Tower.Upgrade[] upgrades;

    private TowerProperties(int attackInterval, double blockRange, double damage, EnemySelector enemySelector, Tower.Upgrade[] upgrades) {
        this.attackInterval = attackInterval;
        this.blockRange = blockRange;
        this.damage = damage;
        this.enemySelector = enemySelector;
        this.upgrades = upgrades;
    }

    /**
     * Amount of ticks inbetween attacks
     * @return
     */
    public int getAttackInterval() {
        return this.attackInterval;
    }

    public double getBlockRange() {
        return this.blockRange;
    }

    public double getDamage() {
        return this.damage;
    }

    public EnemySelector getEnemySelector() {
        return this.enemySelector;
    }

    public static class Builder {

        private int attackInterval;
        private double blockRange;
        private double damage;
        private Tower.Upgrade[] upgrades = new Tower.Upgrade[0];;
        private EnemySelector enemySelector;

        /**
         * Amount of ticks inbetween attacks
         * @param interval ticks
         * @return
         */
        public Builder setAttackInterval(int interval) {
            this.attackInterval = interval;
            return this;
        }

        public Builder setBlockRange(double range) {
            this.blockRange = range;
            return this;
        }

        public Builder setDamage(double damage) {
            this.damage = damage;
            return this;
        }

        public Builder setEnemySelector(EnemySelector selector) {
            this.enemySelector = selector;
            return this;
        }

        public Builder setUpgrades(Tower.Upgrade[] upgrades) {
            this.upgrades = upgrades;
            return this;
        }

        public TowerProperties build() {
            return new TowerProperties(
                    this.attackInterval,
                    this.blockRange,
                    this.damage,
                    this.enemySelector,
                    this.upgrades
            );
        }

    }


}
