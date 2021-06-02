package org.madblock.skywars.powerups;

import cn.nukkit.Player;
import org.madblock.newgamesapi.game.GameBehavior;

public abstract class PowerUp {

    private static int POWERUP_ID = 0;

    protected GameBehavior behaviour;

    protected int id;

    public PowerUp (GameBehavior behaviour) {
        this.behaviour = behaviour;
        this.id = POWERUP_ID++;
    }

    /**
     * Name of the power up
     * @return
     */
    public abstract String getName ();

    /**
     * Description of the power up
     * @return
     */
    public abstract String getDescription ();

    /**
     * Item to be given as a powerup
     * @return
     */
    public abstract int getItemId ();

    /**
     * Logic for the powerup when used.
     */
    public abstract void use (Player user);

    public int getId () {
        return this.id;
    }

}
