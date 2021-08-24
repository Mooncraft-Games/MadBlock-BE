package org.madblock.newgamesapi.game.events;

import org.madblock.newgamesapi.game.deaths.DeathCause;

public class GamePlayerDeathEvent {

    private int respawnSeconds;
    private int respawnImmunitySeconds;
    private boolean showDeathMessage;
    private DeathState deathState;
    private DeathCause deathCause;

    public GamePlayerDeathEvent(DeathCause deathCause){
        this.respawnSeconds = 5; // 5 Seconds default.
        this.respawnImmunitySeconds = 5;
        this.showDeathMessage = true;
        this.deathState = DeathState.MOVE_TO_DEAD_SPECTATORS;
        this.deathCause = deathCause;
    }

    /**
     * The amount of time in seconds to wait before respawning the
     * player. Only effective if deathState is TIMED_RESPAWN.
     * @return the amount of time in seconds.
     */
    public int getRespawnSeconds() { return respawnSeconds; }

    /**
     * The amount of time after respawning where a player has immunity to
     * all forms of damage less than 500 hit points (to allow void/border kills)
     *
     * Only effective for the TIMED_RESPAWN and INSTANT_RESPAWN death
     * types. Otherwise, respawn immunity must be managed by the game.
     *
     * @return the amount of time in seconds.
     */
    public int getRespawnImmunitySeconds() {
        return respawnImmunitySeconds;
    }

    /**
     * Determines if a death message should be displayed for the death.
     * Note that this only shows a death message if it is true AND the
     * death source has deathmessages enabled. (So a border could silently
     * kill, for example)
     * @return true if the deathmessage should be displayed.
     */
    public boolean shouldShowDeathMessage() { return showDeathMessage; }

    /**
     * Determines the behavior of their death through 4 options.
     * @return the current DeathState.
     */
    public DeathState getDeathState() { return deathState; }

    /**
     * Provides the details of a death from a more generic source than
     * Nukkit's own event system.
     * @return the victim's deathcause.
     */
    public DeathCause getDeathCause() {
        return deathCause;
    }

    /**
     * Sets the amount of time in seconds to wait before respawning the
     * player tied to the event. Only effective if deathState is
     * TIMED_RESPAWN.
     * @param respawnSeconds - the amount of time in seconds.
     */
    public GamePlayerDeathEvent setRespawnSeconds(int respawnSeconds) {
        this.respawnSeconds = respawnSeconds;
        return this;
    }

    /**
     * Sets the amount of time in seconds where a player is immune to
     * damage after respawning in a game.
     *
     * @param respawnImmunitySeconds the amount of time in seconds
     */
    public GamePlayerDeathEvent setRespawnImmunitySeconds(int respawnImmunitySeconds) {
        this.respawnImmunitySeconds = respawnImmunitySeconds;
        return this;
    }

    /**
     * Sets if a death message should be displayed for the death.
     * Note that this only shows a death message if it is true AND the
     * death source has deathmessages enabled. (So a border could silently
     * kill, for example)
     * @param showDeathMessage true if a deathmessage should be displayed.
     */
    public GamePlayerDeathEvent setShowDeathMessage(boolean showDeathMessage) {
        this.showDeathMessage = showDeathMessage;
        return this;
    }

    /**
     * Determines the behavior of their death through 4 options.
     * @param deathState - the DeathState.
     */
    public GamePlayerDeathEvent setDeathState(DeathState deathState) {
        this.deathState = deathState;
        return this;
    }



    public enum DeathState{
        MOVE_TO_DEAD_SPECTATORS,
        TIMED_RESPAWN,
        INSTANT_RESPAWN,
        CANCELLED
    }
}
