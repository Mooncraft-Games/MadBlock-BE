package org.madblock.towerwars.menu.types;

import cn.nukkit.Player;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.window.FormWindow;
import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.menu.MenuParameters;

/**
 * MenuTypes should not implement any listeners
 */
public interface MenuType<P extends MenuParameters> {

    String getId();

    /**
     * This is called when a menu is activated for a player.
     * @param player
     * @param behavior
     * @param params
     * @return
     */
    FormWindow createFormForPlayer(Player player, TowerWarsBehavior behavior, P params);

    /**
     * Called when the player responds to a the corresponding menu's form
     * @param player
     * @param behavior
     * @param response
     */
    void handlePlayerResponse(Player player, TowerWarsBehavior behavior, FormResponse response);

    /**
     * Called if the player leaves with a active menu or if the menu is closed without a response.
     * @param player
     */
    void cleanUp(Player player, TowerWarsBehavior behavior);

    /**
     * Called if the game is stopped.
     */
    void cleanUp();

}
