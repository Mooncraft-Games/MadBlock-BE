package org.madblock.towerwars.kits.features;

import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.item.ItemID;
import cn.nukkit.level.Position;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.kits.ExtendedKit;
import org.madblock.towerwars.behaviors.TowerWarsBehavior;
import org.madblock.towerwars.menu.types.MonsterListMenuType;
import org.madblock.towerwars.menu.types.TowerListMenuType;

public class DefaultKitFeatures extends ExtendedKit {

    private static final long INTERACT_COOLDOWN = 1000L;

    private long lastInteractEvent; // The interact event is spammed by the client so we need a cooldown.

    @EventHandler
    public void onItemUsage(PlayerInteractEvent event) {

        if (event.getPlayer().equals(this.getTarget())) {

            TowerWarsBehavior behavior = (TowerWarsBehavior)this.gameHandler.getGameBehaviors();

            switch (event.getItem().getId()) {
                case ItemID.ARMOR_STAND:    // Spawn towers
                    event.setCancelled();
                    if (!isOffInteractCooldown()) {
                        return;
                    }
                    this.lastInteractEvent = System.currentTimeMillis();

                    if (behavior.getSessionHandler().getGameState() != GameHandler.GameState.MAIN_LOOP) {
                        event.getPlayer().sendMessage(Utility.generateServerMessage("Game", TextFormat.BLUE, "Sorry, you can only use this during the game.", TextFormat.RED));
                        return;
                    }

                    // Only allow tower spawning on a map specific block
                    int towerPlacementBlockId = behavior
                            .getSessionHandler()
                            .getPrimaryMapID()
                            .getIntegers()
                            .getOrDefault("tower_block_id", 0);
                    if (event.getBlock().getId() != towerPlacementBlockId) {
                        event.getPlayer().sendMessage(Utility.generateServerMessage("Game", TextFormat.BLUE, "Sorry, you can only build towers on a " + Block.get(towerPlacementBlockId).getName().toLowerCase() + ".", TextFormat.RED));
                        return;
                    }

                    Position towerPosition = Position.fromObject(event.getBlock().getLocation().add(0.5d, 1, 0.5d), event.getBlock().getLevel());
                    behavior.getMenuManager().showMenu(event.getPlayer(), TowerListMenuType.ID, new TowerListMenuType.TowerListMenuParameters(towerPosition));
                    break;

                case ItemID.SPAWN_EGG:      // Spawn monsters
                    event.setCancelled();
                    if (!isOffInteractCooldown()) {
                        return;
                    }
                    this.lastInteractEvent = System.currentTimeMillis();

                    if (behavior.getSessionHandler().getGameState() != GameHandler.GameState.MAIN_LOOP) {
                        event.getPlayer().sendMessage(Utility.generateServerMessage("Game", TextFormat.BLUE, "Sorry, you can only use this during the game.", TextFormat.RED));
                        return;
                    }
                    behavior.getMenuManager().showMenu(event.getPlayer(), MonsterListMenuType.ID);
                    break;
            }

        }
    }

    private boolean isOffInteractCooldown() {
        return this.lastInteractEvent + INTERACT_COOLDOWN < System.currentTimeMillis();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer().equals(this.target)) {
            event.setCancelled();
        }
    }

}
