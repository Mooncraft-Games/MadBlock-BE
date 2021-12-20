package org.madblock.towerwars.menu.types;

import cn.nukkit.Player;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.level.Position;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.util.Utility;
import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.menu.MenuParameters;
import org.madblock.towerwars.towers.types.TowerType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class TowerListMenuType implements MenuType {

    public static final String ID = "tower_list_menu";

    // Stores the options available for the player.
    private final Map<UUID, TowerListInternalMenuData> openMenus = new HashMap<>();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public FormWindow createFormForPlayer(Player player, TowerWarsBehavior behavior, MenuParameters params) {
        TowerListMenuData data = (TowerListMenuData)params;
        FormWindowSimple form = new FormWindowSimple("Purchase Tower - " + TextFormat.GOLD + behavior.getBalance(player) + " " + Utility.ResourcePackCharacters.COIN, "");

        // Lowest to highest in cost, if they have the same cost sort alphabetically.
        List<TowerType> types = behavior.getUnlockedTowerTypes(player)
            .stream()
            .sorted((towerTypeA, towerTypeB) -> {
                if (towerTypeA.getCost() == towerTypeB.getCost()) {
                    return towerTypeA.getName().compareTo(towerTypeB.getName());
                }
                return towerTypeA.getCost() - towerTypeB.getCost();
            })
            .collect(Collectors.toList());

        for (TowerType towerType : types) {
            ElementButton button = new ElementButton((behavior.getBalance(player) < towerType.getCost() ? TextFormat.RED : "") + towerType.getName() + "\n" + TextFormat.GOLD + towerType.getCost() + " GOLD");
            form.addButton(button);
        }

        this.openMenus.put(player.getUniqueId(), new TowerListInternalMenuData(data.getBuildPosition(), types));
        return form;
    }

    @Override
    public void handlePlayerResponse(Player player, TowerWarsBehavior behavior, FormResponse response) {
        FormResponseSimple simpleResponse = (FormResponseSimple)response;

        TowerListInternalMenuData menuParameters = this.openMenus.get(player.getUniqueId());
        TowerType chosenType = menuParameters.getTowers().get(simpleResponse.getClickedButtonId());
        this.openMenus.remove(player.getUniqueId());

        TowerPurchaseMenuType.TowerPurchaseMenuData params = new TowerPurchaseMenuType.TowerPurchaseMenuData(chosenType, menuParameters.getBuildPosition());
        behavior.getMenuManager().showMenu(player, TowerPurchaseMenuType.ID, params);
    }

    @Override
    public void cleanUp(Player player, TowerWarsBehavior behavior) {
        this.openMenus.remove(player.getUniqueId());
    }

    @Override
    public void cleanUp() {
        this.openMenus.clear();
    }

    public static class TowerListMenuData implements MenuParameters {

        private final Position position;

        public TowerListMenuData(Position position) {
            this.position = position;
        }

        public Position getBuildPosition() {
            return this.position;
        }

    }

    // Used to add towers list parameter
    private static class TowerListInternalMenuData extends TowerListMenuData {

        private final List<TowerType> towers;

        public TowerListInternalMenuData(Position position, List<TowerType> towers) {
            super(position);
            this.towers = towers;
        }

        public List<TowerType> getTowers() {
            return this.towers;
        }

    }

}
