package org.madblock.towerwars.towers.tower;

import cn.nukkit.entity.Entity;
import cn.nukkit.event.Listener;
import cn.nukkit.level.Position;
import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.enemies.enemy.Enemy;
import org.madblock.towerwars.events.enemy.tower.EnemyUntargettedEvent;
import org.madblock.towerwars.events.GameListener;
import org.madblock.towerwars.towers.effects.TowerEffect;
import org.madblock.towerwars.events.tower.targets.TowerTargetAttackEvent;
import org.madblock.towerwars.events.tower.targets.TowerTargetSelectEvent;
import org.madblock.towerwars.utils.GameRegion;

import java.util.*;

public abstract class Tower implements GameListener, Listener {

    protected final TowerWarsBehavior behavior;
    private final GameRegion gameRegion;

    private Entity entity;
    private int attackDelayTicks;    // Ticks until we can attack again

    private final TowerProperties properties;

    private final List<TowerEffect> effects = new ArrayList<>();
    private Set<Enemy> targets = new HashSet<>();

    public Tower(TowerProperties properties, TowerWarsBehavior behavior, GameRegion gameRegion) {
        this.behavior = behavior;
        this.gameRegion = gameRegion;
        this.properties = properties;
        behavior.getEventManager().register(this);
    }

    public TowerProperties getProperties() {
        return this.properties;
    }

    public void build(Position position) {
        this.entity = this.createEntity(position);
    }

    /**
     * Called when the tower is purchased is told be be built
     * You do not need to spawn the entity.
     * @param position
     * @return Tower entity
     */
    protected abstract Entity createEntity(Position position);

    public Upgrade[] getTowerUpgrades() {
        return new Upgrade[]{};
    }

    public void cleanUp() {
        this.entity.kill();
        this.getBehavior().getEventManager().unregister(this);
        for (Enemy target : this.targets) {
            EnemyUntargettedEvent event = new EnemyUntargettedEvent(this.behavior, target);
            this.behavior.getEventManager().callEvent(event);
        }
    }

    public void addEffect(TowerEffect effect) {
        this.effects.add(effect);
    }

    public List<TowerEffect> getEffects() {
        return Collections.unmodifiableList(this.effects);
    }

    public TowerWarsBehavior getBehavior() {
        return this.behavior;
    }

    public GameRegion getGameRegion() {
        return this.gameRegion;
    }

    public Entity getEntity() {
        return this.entity;
    }

    public void attack(Enemy enemy) {
        TowerTargetAttackEvent event = new TowerTargetAttackEvent(this.behavior, this, enemy, this.properties.getDamage());
        this.getBehavior().getEventManager().callEvent(event);
        if (!event.isCancelled()) {
            this.attackDelayTicks = this.properties.getAttackInterval();
            enemy.attacked(this, this.properties.getDamage());
        }
    }

    public void tick() {
        // Get our targets
        Set<Enemy> enemies = this.behavior.getActiveEnemies(this.gameRegion);
        Set<Enemy> targets = this.properties.getEnemySelector().getTargets(this, enemies);
        if (!targets.equals(this.targets)) {    // Only trigger event if new targets are chosen

            // Select all new towers
            Set<Enemy> newTargets = new HashSet<>();
            for (Enemy target : targets) {
                if (!this.targets.contains(target)) {
                    TowerTargetSelectEvent event = new TowerTargetSelectEvent(this.behavior, this, target);
                    this.getBehavior().getEventManager().callEvent(event);
                    if (!event.isCancelled()) {
                        newTargets.add(target);
                    }
                }
            }

            // Remove old towers that are no longer selected
            this.targets.stream()
                    .filter(target -> !newTargets.contains(target))
                    .forEach(target -> {
                        EnemyUntargettedEvent event = new EnemyUntargettedEvent(this.behavior, target);
                        this.getBehavior().getEventManager().callEvent(event);
                        if (event.isCancelled()) {
                            newTargets.add(target);
                        }
                    });
            this.targets = newTargets;
        }

        if (this.attackDelayTicks > 0) {
            this.attackDelayTicks--;
        }

        // Attack if able
        if (this.targets.size() > 0 && this.attackDelayTicks <= 0) {
            for (Enemy target : this.targets) {
                this.attack(target);
            }
        }

    }

    public static class Upgrade {

        private final String towerId;
        private final int cost;

        public Upgrade(String towerId, int cost) {
            this.towerId = towerId;
            this.cost = cost;
        }

        public String getTowerId() {
            return this.towerId;
        }

        public int getCost() {
            return this.cost;
        }

    }

}
