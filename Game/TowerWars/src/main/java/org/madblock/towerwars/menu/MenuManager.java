package org.madblock.towerwars.menu;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import org.madblock.towerwars.TowerWarsPlugin;
import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.menu.types.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MenuManager implements Listener {

    private final static Map<String, MenuType> menuTypes = new HashMap<>();

    private final Map<Integer, MenuType> forms = new HashMap<>();
    private final Map<UUID, Integer> formOwnerships = new HashMap<>();

    private final TowerWarsBehavior behavior;

    static {
        register(new TowerPurchaseMenuType());
        register(new TowerListMenuType());
        register(new MonsterListMenuType());
        register(new MonsterPurchaseMenuType());
    }


    public MenuManager(TowerWarsBehavior behavior) {
        this.behavior = behavior;
        Server.getInstance().getPluginManager().registerEvents(this, TowerWarsPlugin.get());
    }

    public void showMenu(Player player, String menuId) {
        this.showMenu(player, menuId, null);
    }

    public void showMenu(Player player, String menuId, MenuParameters params) {
        if (menuTypes.containsKey(menuId)) {
            this.handlePlayerMenuCleanUp(player);
            MenuType<MenuParameters> menu = menuTypes.get(menuId);
            int formId = player.showFormWindow(menu.createFormForPlayer(player, this.behavior, params));
            this.forms.put(formId, menu);
            this.formOwnerships.put(player.getUniqueId(), formId);
        } else {
            throw new NullPointerException("Invalid menuId specified. " + menuId);
        }
    }

    private static void register(MenuType<? extends MenuParameters> menuType) {
        menuTypes.put(menuType.getId(), menuType);
    }

    /**
     * Cleanup all form data
     */
    public void cleanUp() {
        menuTypes.forEach((k, menuType) -> menuType.cleanUp());
        this.forms.clear();
        HandlerList.unregisterAll(this);
    }

    /**
     * Cleanup form data for a player
     * @param player
     */
    public void handlePlayerMenuCleanUp(Player player) {
        if (this.formOwnerships.containsKey(player.getUniqueId())) {
            int formId = this.formOwnerships.get(player.getUniqueId());
            this.forms.get(formId).cleanUp(player, this.behavior);

            this.forms.remove(formId);
            this.formOwnerships.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onFormResponse(PlayerFormRespondedEvent event) {
        if (this.forms.containsKey(event.getFormID())) {
            MenuType menuType = this.forms.get(event.getFormID());
            this.forms.remove(event.getFormID());
            this.formOwnerships.remove(event.getPlayer().getUniqueId());

            if (event.wasClosed()) {
                this.handlePlayerMenuCleanUp(event.getPlayer());
                return;
            }
            menuType.handlePlayerResponse(event.getPlayer(), this.behavior, event.getResponse());
        }
    }

}
