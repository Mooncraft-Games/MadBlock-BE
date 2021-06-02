package org.madblock.newgamesapi.game.events;

import org.madblock.newgamesapi.game.deaths.DeathCause;

public class GamePlayerDeathEvent {

    private int respawnSeconds;
    private boolean showDeathMessage;
    private DeathState deathState;
    private DeathCause deathCause;

    public GamePlayerDeathEvent(DeathCause deathCause){
        this.respawnSeconds = 100; // 5 Seconds default.
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
     * @param respawnSeconds - the amount of time in ticks.
     */
    public void setRespawnSeconds(int respawnSeconds) { this.respawnSeconds = respawnSeconds; }

    /**
     * Sets if a death message should be displayed for the death.
     * Note that this only shows a death message if it is true AND the
     * death source has deathmessages enabled. (So a border could silently
     * kill, for example)
     * @param showDeathMessage true if a deathmessage should be displayed.
     */
    public void setShowDeathMessage(boolean showDeathMessage) { this.showDeathMessage = showDeathMessage; }

    /**
     * Determines the behavior of their death through 4 options.
     * @param deathState - the DeathState.
     */
    public void setDeathState(DeathState deathState) { this.deathState = deathState; }

    public enum DeathState{
        MOVE_TO_DEAD_SPECTATORS,
        TIMED_RESPAWN,
        INSTANT_RESPAWN,
        CANCELLED
    }
}
