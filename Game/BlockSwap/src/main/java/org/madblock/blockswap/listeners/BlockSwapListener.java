package org.madblock.blockswap.listeners;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.inventory.InventoryTransactionEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.inventory.Inventory;
import cn.nukkit.item.Item;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.utils.TextFormat;
import org.madblock.blockswap.behaviours.BlockSwapGameBehaviour;
import org.madblock.blockswap.utils.BlockSwapConstants;
import org.madblock.newgamesapi.util.Utility;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.team.TeamPresets;

import java.util.*;

public class BlockSwapListener implements Listener {


    protected final List<UUID> cooldown = new ArrayList<>(); // Required bc win 10 is wack. Triggers interact event multiple times.
    protected final BlockSwapGameBehaviour gameBehaviour;
    protected final GameHandler handler;

    public BlockSwapListener(BlockSwapGameBehaviour behaviour) {
        this.gameBehaviour = behaviour;
        this.handler = gameBehaviour.getSessionHandler();
    }


    @EventHandler
    public void onItemMovement(InventoryTransactionEvent event) {
        Player player = event.getTransaction().getSource();

        if (handler.getPlayers().contains(player) && (player.getGamemode() != Player.CREATIVE)) {
            event.setCancelled();
        }
    }

    @EventHandler
    public void onAbilityUsage(PlayerInteractEvent event) {
        Item item = event.getItem();

        if (this.handler.getPlayers().contains(event.getPlayer()) && item.hasCompoundTag()) {
            if (!this.handler.getGameState().equals(GameHandler.GameState.MAIN_LOOP)) return;
            if (this.cooldown.contains(event.getPlayer().getLoginChainData().getClientUUID())) return;

            CompoundTag nbtTag = item.getNamedTag();
            String tag = nbtTag.getString("ability");

            if (tag.equals("leap")) {
                this.cooldown.add(event.getPlayer().getLoginChainData().getClientUUID());
                handler.getGameScheduler().registerGameTask(() -> this.cooldown.remove(event.getPlayer().getLoginChainData().getClientUUID()), 10);

                if (!nbtTag.exist("ench")) {
                    event.getPlayer().sendMessage(Utility.generateServerMessage("ABILITY", TextFormat.RED, "This ability is still on cooldown!"));
                    return;
                }

                Vector3 directionVector = event.getPlayer().getDirectionVector();
                event.getPlayer().setMotion(new Vector3(directionVector.getX(), Math.abs(directionVector.getY() / 2), directionVector.getZ()).multiply(BlockSwapConstants.LEAP_STRENGTH));
                int itemIndex = event.getPlayer().getInventory().getHeldItemIndex();

                this.handler.getGameScheduler().registerGameTask(() -> {
                    if (!this.handler.getTeams().get(TeamPresets.DEAD_TEAM_ID).getPlayers().contains(event.getPlayer())) {
                        nbtTag.putList(new ListTag<>("ench"));
                        item.setCompoundTag(nbtTag);
                        Inventory inventory = event.getPlayer().getInventory();
                        inventory.setItem(itemIndex, item);
                        inventory.sendContents(event.getPlayer());
                    }
                }, 40);

                nbtTag.remove("ench");
                item.setCompoundTag(nbtTag);
                event.getPlayer().getInventory().setItemInHand(item);
                event.getPlayer().getInventory().sendContents(event.getPlayer());
            } else if (tag.equals("power_up")) {
                int slot;
                switch (event.getPlayer().getInventory().getHeldItemIndex()) {
                    case 3:
                        slot = 0;
                        break;
                    case 4:
                        slot = 1;
                        break;
                    case 5:
                        slot = 2;
                        break;
                    default:
                        event.getPlayer().sendMessage(Utility.generateServerMessage("ERROR", TextFormat.DARK_RED, "Misconfigured power up slot.", TextFormat.RED));
                        return;
                }
                if (this.gameBehaviour.getPowerUpManager().getPowerUp(event.getPlayer(), slot).isPresent()) {
                    this.gameBehaviour.getPowerUpManager().usePowerUp(event.getPlayer(), slot);
                }
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity().getLevel().getId() == this.handler.getPrimaryMap().getId()) {
            event.getEntity().extinguish();
            event.setCancelled();
        }
    }

}
