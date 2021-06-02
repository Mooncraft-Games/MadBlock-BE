package org.madblock.blockswap.powerups;

import cn.nukkit.Player;
import org.madblock.newgamesapi.game.GameBehavior;

public abstract class PowerUp {

    protected GameBehavior behaviour;

    protected Player player;

    public PowerUp (GameBehavior behaviour, Player player) {
        this.behaviour = behaviour;
        this.player = player;
    }

    /**
     * @return Display name of the powerup.
     */
    public abstract String getName ();

    /**
     * @return Short description of the powerup's ability
     */
    public abstract String getDescription ();

    /**
     * @return If the powerup should be used immediately or when the player interacts with the environment when they receive it
     */
    public abstract boolean isInstantConsumable ();

    /**
     * Logic for the powerup.
     */
    public abstract void use ();

}
