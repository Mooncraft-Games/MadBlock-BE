package org.madblock.towerwars.events;

import cn.nukkit.event.Cancellable;
import org.madblock.towerwars.behaviors.TowerWarsBehavior;

public abstract class GameEvent implements Cancellable {

    private boolean cancelled;

    private final TowerWarsBehavior behavior;

    public GameEvent(TowerWarsBehavior behavior) {
        this.behavior = behavior;
    }

    public TowerWarsBehavior getGameBehavior() {
        return this.behavior;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled() {
        this.cancelled = true;
    }

    @Override
    public void setCancelled(boolean forceCancel) {
        this.cancelled = forceCancel;
    }

}
