package org.madblock.towerwars.menu.types;

import cn.nukkit.Player;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.Utility;
import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.enemies.types.EnemyType;
import org.madblock.towerwars.menu.MenuParameters;

import java.util.*;
import java.util.stream.Collectors;

public class MonsterListMenuType implements MenuType<MonsterListMenuType.MonsterListMenuParameters> {

    public static final String ID = "monster_list_menu";

    private final Map<UUID, MonsterListMenuParametersInternal> openMenus = new HashMap<>();


    @Override
    public String getId() {
        return ID;
    }

    @Override
    public FormWindow createFormForPlayer(Player player, TowerWarsBehavior behavior, MonsterListMenuParameters params) {
        FormWindowSimple form = new FormWindowSimple(
                "Purchase Monster - " + TextFormat.GOLD + behavior.getBalance(player) + " " + Utility.ResourcePackCharacters.COIN,
                "These monsters are sent to your enemies!");

        // Lowest to highest in cost, if they have the same cost sort alphabetically.
        List<EnemyType> types = behavior.getUnlockedEnemyTypes(player)
                .stream()
                .sorted((enemyTypeA, enemyTypeB) -> {
                    if (enemyTypeA.getCost() == enemyTypeB.getCost()) {
                        return enemyTypeA.getName().compareTo(enemyTypeB.getName());
                    }
                    return enemyTypeA.getCost() - enemyTypeB.getCost();
                })
                .collect(Collectors.toList());

        for (EnemyType enemyType : types) {
            ElementButton button = new ElementButton((behavior.getBalance(player) < enemyType.getCost() ? TextFormat.RED : "") + enemyType.getName() + "\n" + TextFormat.GOLD + enemyType.getCost() + " GOLD");
            form.addButton(button);
        }

        this.openMenus.put(player.getUniqueId(), new MonsterListMenuParametersInternal(types));
        return form;
    }

    @Override
    public void handlePlayerResponse(Player player, TowerWarsBehavior behavior, FormResponse response) {
        FormResponseSimple simpleResponse = (FormResponseSimple)response;
        MonsterListMenuParametersInternal params = this.openMenus.get(player.getUniqueId());
        EnemyType chosenType = params.getTypes().get(simpleResponse.getClickedButtonId());
        this.openMenus.remove(player.getUniqueId());

        MonsterPurchaseMenuType.MonsterPurchaseMenuParameters parameters = new MonsterPurchaseMenuType.MonsterPurchaseMenuParameters(chosenType);
        behavior.getMenuManager().showMenu(player, MonsterPurchaseMenuType.ID, parameters);
    }

    @Override
    public void cleanUp(Player player, TowerWarsBehavior behavior) {
        this.openMenus.remove(player.getUniqueId());
    }

    @Override
    public void cleanUp() {
        this.openMenus.clear();
    }

    private static class MonsterListMenuParametersInternal implements MenuParameters {

        private final List<EnemyType> types;

        public MonsterListMenuParametersInternal(List<EnemyType> types) {
            this.types = types;
        }

        public List<EnemyType> getTypes() {
            return this.types;
        }

    }

    public static class MonsterListMenuParameters implements MenuParameters {

    }

}
