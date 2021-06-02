package org.madblock.towerwars.enemies.enemy;

import cn.nukkit.entity.Entity;
import cn.nukkit.level.Position;
import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.enemies.effects.EnemyEffect;
import org.madblock.towerwars.events.GameListener;
import org.madblock.towerwars.towers.tower.Tower;
import org.madblock.towerwars.utils.GameRegion;

import java.util.*;

public abstract class Enemy implements GameListener {

    protected final TowerWarsBehavior behavior;
    private final GameRegion gameRegion;

    private Entity entity;

    private final List<EnemyEffect> effects = new ArrayList<>();
    private final Set<Tower> viewers = new HashSet<>();

    public Enemy(TowerWarsBehavior behavior, GameRegion gameRegion) {
        this.behavior = behavior;
        this.gameRegion = gameRegion;
        behavior.getEventManager().register(this);
    }

    public void spawn(Position position) {
        this.entity = this.createEntity(position);
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

    public void tick() {

    }

    public void addViewer(Tower tower) {
        this.viewers.add(tower);
    }

    public void removeViewer(Tower tower) {
        this.viewers.remove(tower);
    }

}
