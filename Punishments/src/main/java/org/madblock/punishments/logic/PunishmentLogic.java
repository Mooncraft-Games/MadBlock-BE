package org.madblock.punishments.logic;

import cn.nukkit.Player;
import org.madblock.punishments.api.PunishmentEntry;
import org.madblock.punishments.enums.PunishmentType;

/**
 * Serves to contain the logic for a punishment type.
 */
public abstract class PunishmentLogic {

    private PunishmentType type;

    public PunishmentLogic (PunishmentType type) {
        this.type = type;
    }

    public PunishmentType getType () {
        return this.type;
    }

    /**
     * Called when a player sends a message.
     * @param player
     * @return whether or not the player's message should be sent to other players.
     */
    public boolean onChat (Player player, PunishmentEntry punishment) {
        return true;
    }

    /**
     * Called when a player joins the server.
     * @param player
     * @return whether or not to call onJoin for other punishments the player has
     */
    public boolean onJoin (Player player, PunishmentEntry punishment) {
        return true;
    }

    /**
     * Called when a player is punished.
     * @param player
     * @return
     */
    public void onPunish (Player player, PunishmentEntry punishment) {}

}
