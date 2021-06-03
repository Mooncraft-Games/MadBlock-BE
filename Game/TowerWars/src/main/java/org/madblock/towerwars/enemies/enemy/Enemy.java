package org.madblock.towerwars.enemies.enemy;

import cn.nukkit.entity.Entity;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import org.madblock.towerwars.TowerWarsPlugin;
import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.enemies.effects.EnemyEffect;
import org.madblock.towerwars.enemies.events.states.EnemyMoveEvent;
import org.madblock.towerwars.enemies.events.states.EnemyTakeLifeEvent;
import org.madblock.towerwars.events.GameListener;
import org.madblock.towerwars.pathfinding.Vector2;
import org.madblock.towerwars.towers.tower.Tower;
import org.madblock.towerwars.utils.EntityUtils;
import org.madblock.towerwars.utils.GameRegion;

import java.util.*;

public abstract class Enemy implements GameListener {

    private final GameRegion gameRegion;
    private final EnemyProperties properties;
    protected final TowerWarsBehavior behavior;

    private Entity entity;

    private final List<EnemyEffect> effects = new ArrayList<>();

    private List<Vector2> path = Collections.emptyList();
    private int currentPathIndex = 0;

    public Enemy(EnemyProperties properties, TowerWarsBehavior behavior, GameRegion gameRegion) {
        this.properties = properties;
        this.behavior = behavior;
        this.gameRegion = gameRegion;
        behavior.getEventManager().register(this);
    }

    public EnemyProperties getProperties() {
        return this.properties;
    }

    public void spawn(Position position) {
        this.entity = this.createEntity(position);
        this.requestPathfinding();
        this.entity.spawnToAll();
    }

    /**
     * Called when the enemy is to be spawned on the world.
     * You do not need to call spawnToAll
     * @param position
     * @return the entity that represents this enemy
     */
    protected abstract Entity createEntity(Position position);

    public Entity getEntity() {
        return this.entity;
    }

    /**
     * Request the pathfinder to find a path for this enemy to get to the end region
     */
    public void requestPathfinding() {
        this.behavior.getPathfinder().solve(this.gameRegion, this.entity.getPosition()).whenComplete((path, exception) -> {
            if (exception != null) {
                TowerWarsPlugin.get().getLogger().error("Failed to pathfind.", exception);
                this.cleanUp();
            } else {
                this.path = path;
                this.currentPathIndex = 0;
            }
        });
    }

    public GameRegion getGameRegion() {
        return this.gameRegion;
    }

    public void addEffect(EnemyEffect effect) {
        this.effects.add(effect);
    }

    public void attacked(Tower tower, double damage) {

    }

    public void cleanUp() {
        this.behavior.getEventManager().unregister(this);
        this.entity.kill();
    }

    public void kill() {
        this.cleanUp();
    }

    public void tick() {

        // Follow pathfinding to next location
        if (this.path.size() > 0 && this.currentPathIndex < this.path.size()) {
            Vector2 nextVector = this.path.get(this.currentPathIndex);
            if ((int)this.entity.getX() == (int)nextVector.getX() && (int)this.entity.getZ() == (int)nextVector.getZ()) {
                // They already reached their destination. Next index!
                this.currentPathIndex++;
                nextVector = this.path.get(this.currentPathIndex);
            }
            Vector2 movement = this.getMovementToGotoVector(nextVector);

            EnemyMoveEvent event = new EnemyMoveEvent(this.behavior, this, movement);
            this.behavior.getEventManager().callEvent(event);
            if (!event.isCancelled()) {
                this.entity.setPosition(this.entity.getPosition().add(event.getMovementVector().getX(), 0, event.getMovementVector().getZ()));
                EntityUtils.lookAt(this.entity, new Vector3(event.getMovementVector().getX(), this.entity.getY() + 1, event.getMovementVector().getZ()));
            }
        }

        // Take player lives if we are in the end region.
        if (this.gameRegion.getEndGoalArea().isWithinThisRegion(new Vector3(this.entity.getX(), this.gameRegion.getEndGoalArea().getPosLesser().getY(), this.entity.getZ()))) {
            EnemyTakeLifeEvent event = new EnemyTakeLifeEvent(this.behavior, this, this.getProperties().getLivesCost());
            this.behavior.getEventManager().callEvent(event);
            if (!event.isCancelled()) {
                this.behavior.setLives(this.behavior.getGameRegionOwner(this.gameRegion), -event.getLivesCost());
                this.kill();
            }
        }

    }

    private Vector2 getMovementToGotoVector(Vector2 targetVector) {
        double x = targetVector.getX() > this.entity.getX() ? this.properties.getMovementPerTick() : -this.properties.getMovementPerTick();
        double z = targetVector.getZ() > this.entity.getZ() ? this.properties.getMovementPerTick() : -this.properties.getMovementPerTick();
        return new Vector2(x, z);
    }

}