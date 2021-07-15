package org.madblock.towerwars.menu.types;

import cn.nukkit.Player;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.response.FormResponseModal;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowModal;
import cn.nukkit.level.Position;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.Utility;
import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.menu.MenuParameters;
import org.madblock.towerwars.events.tower.states.TowerCreationEvent;
import org.madblock.towerwars.towers.tower.Tower;
import org.madblock.towerwars.towers.tower.TowerProperties;
import org.madblock.towerwars.towers.types.TowerType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TowerPurchaseMenuType implements MenuType<TowerPurchaseMenuType.TowerPurchaseMenuParameters> {

    public static final String ID = "tower_purchase_menu";

    private final Map<UUID, TowerPurchaseMenuParameters> openMenus = new HashMap<>();


    @Override
    public String getId() {
        return ID;
    }

    @Override
    public FormWindow createFormForPlayer(Player player, TowerWarsBehavior behavior, TowerPurchaseMenuParameters params) {
        TowerType towerType = params.getTowerType();
        TowerProperties properties = towerType.getTowerProperties();

        String formContent = towerType.getDescription() + "\n" +
                            "Damage: " + properties.getDamage() + "\n" +
                            "Range: " + properties.getBlockRange() + "\n" +
                            "Attack Interval: " + (properties.getAttackInterval() / 20d) + "\n" +
                            "Target: " + properties.getEnemySelector().getTargetDescription();


        FormWindowModal form = new FormWindowModal(
                towerType.getName() + " - Information",
                formContent,
                "Build",
                "Cancel"
        );

        this.openMenus.put(player.getUniqueId(), params);
        return form;
    }

    @Override
    public void handlePlayerResponse(Player player, TowerWarsBehavior behavior, FormResponse response) {
        FormResponseModal formResponse = (FormResponseModal)response;
        if (formResponse.getClickedButtonId() == 0) {
            // Build
            TowerPurchaseMenuParameters data = this.openMenus.get(player.getUniqueId());
            this.openMenus.remove(player.getUniqueId());

            if (behavior.getBalance(player) < data.getTowerType().getCost()) {
                player.sendMessage(Utility.generateServerMessage("Game", TextFormat.BLUE, "You do not have enough gold!", TextFormat.RED));
                return;
            }

            Tower tower = data.getTowerType().create(behavior.getPlayerGameRegion(player));
            TowerCreationEvent event = new TowerCreationEvent(behavior, tower);
            behavior.getEventManager().callEvent(event);
            if (!event.isCancelled()) {
                behavior.setBalance(player, behavior.getBalance(player) - data.getTowerType().getCost());
                tower.build(data.getBuildPosition());
                behavior.addTower(tower);
            }

        }
    }

    @Override
    public void cleanUp(Player player, TowerWarsBehavior behavior) {
        this.openMenus.remove(player.getUniqueId());
    }

    @Override
    public void cleanUp() {
        this.openMenus.clear();
    }


    public static class TowerPurchaseMenuParameters implements MenuParameters {

        private final TowerType towerType;
        private final Position position;


        public TowerPurchaseMenuParameters(TowerType towerType, Position position) {
            this.towerType = towerType;
            this.position = position;
        }

        public TowerType getTowerType() {
            return this.towerType;
        }

        public Position getBuildPosition() {
            return this.position;
        }

    }

}
