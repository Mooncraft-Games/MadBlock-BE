package org.madblock.towerwars.menu.types;

import cn.nukkit.Player;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.response.FormResponseModal;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowModal;
import cn.nukkit.level.Position;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.map.types.MapRegion;
import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.enemies.enemy.Enemy;
import org.madblock.towerwars.enemies.enemy.EnemyProperties;
import org.madblock.towerwars.enemies.types.EnemyType;
import org.madblock.towerwars.events.enemy.states.EnemySpawnEvent;
import org.madblock.towerwars.menu.MenuParameters;
import org.madblock.towerwars.utils.GameRegion;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MonsterPurchaseMenuType implements MenuType<MonsterPurchaseMenuType.MonsterPurchaseMenuParameters> {

    public static final String ID = "monster_purchase_menu";

    private final Map<UUID, MonsterPurchaseMenuParameters> openMenus = new HashMap<>();

    
    @Override
    public String getId() {
        return ID;
    }

    @Override
    public FormWindow createFormForPlayer(Player player, TowerWarsBehavior behavior, MonsterPurchaseMenuParameters params) {
        EnemyType enemyType = params.getType();
        EnemyProperties properties = enemyType.getProperties();

        String formContent = enemyType.getDescription() + "\n" +
                "Health: " + properties.getHealth() + "\n" +
                "Movement Speed: " + properties.getMovementPerTick() + " blocks per second\n" +
                "Lifesteal: " + properties.getLivesCost() + " Lives";


        FormWindowModal form = new FormWindowModal(
                enemyType.getName() + " - Information",
                formContent,
                "Spawn",
                "Cancel"
        );

        this.openMenus.put(player.getUniqueId(), params);
        return form;
    }

    @Override
    public void handlePlayerResponse(Player player, TowerWarsBehavior behavior, FormResponse response) {
        FormResponseModal formResponse = (FormResponseModal)response;
        if (formResponse.getClickedButtonId() == 0) {
            // Spawn
            MonsterPurchaseMenuParameters data = this.openMenus.get(player.getUniqueId());
            this.openMenus.remove(player.getUniqueId());

            if (behavior.getBalance(player) < data.getType().getCost()) {
                player.sendMessage(Utility.generateServerMessage("Game", TextFormat.BLUE, "You do not have enough gold!", TextFormat.RED));
                return;
            }

            boolean spawnedInAny = false;   // Whether or not the enemy was spawned in any play area
            for (GameRegion gameRegion : behavior.getActiveGameRegions()) {
                if (!gameRegion.equals(behavior.getPlayerGameRegion(player))) {
                    Enemy enemy = data.getType().create(gameRegion);

                    EnemySpawnEvent event = new EnemySpawnEvent(behavior, enemy);
                    behavior.getEventManager().callEvent(event);
                    if (!event.isCancelled()) {
                        spawnedInAny = true;
                        enemy.spawn(getRandomSpawnLocation(behavior, gameRegion));
                        behavior.addEnemy(enemy);
                    }
                }
            }
            if (spawnedInAny) {
                behavior.setBalance(player, behavior.getBalance(player) - data.getType().getCost());
            }

        }
    }

    /**
     * Retrieve a random start position for mobs to spawn in
     * @param behavior
     * @param gameRegion
     * @return
     */
    private static Position getRandomSpawnLocation(TowerWarsBehavior behavior, GameRegion gameRegion) {
        MapRegion spawnArea = gameRegion.getSpawnMonstersArea();
        int x = (int)Math.floor(Math.random() * (spawnArea.getPosGreater().getX() - spawnArea.getPosLesser().getX())) + spawnArea.getPosLesser().getX();
        int y = (int)Math.floor(Math.random() * (spawnArea.getPosGreater().getY() - spawnArea.getPosLesser().getY())) + spawnArea.getPosLesser().getY();
        int z = (int)Math.floor(Math.random() * (spawnArea.getPosGreater().getZ() - spawnArea.getPosLesser().getZ())) + spawnArea.getPosLesser().getZ();
        return new Position(x, y, z, behavior.getSessionHandler().getPrimaryMap());
    }

    @Override
    public void cleanUp(Player player, TowerWarsBehavior behavior) {
        this.openMenus.remove(player.getUniqueId());
    }

    @Override
    public void cleanUp() {
        this.openMenus.clear();
    }


    public static class MonsterPurchaseMenuParameters implements MenuParameters {

        private final EnemyType enemyType;


        public MonsterPurchaseMenuParameters(EnemyType enemyType) {
            this.enemyType = enemyType;
        }

        public EnemyType getType() {
            return this.enemyType;
        }

    }

}
