package org.madblock.blockswap.listeners;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import cn.nukkit.event.inventory.InventoryTransactionEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.inventory.Inventory;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Sound;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.utils.TextFormat;
import org.madblock.blockswap.BlockSwapPlugin;
import org.madblock.blockswap.behaviours.BlockSwapGameBehaviour;
import org.madblock.blockswap.powerups.PowerUp;
import org.madblock.blockswap.utils.BlockSwapConstants;
import org.madblock.blockswap.utils.BlockSwapUtility;
import org.madblock.newgamesapi.NewGamesAPI1;
import org.madblock.newgamesapi.Utility;
import org.madblock.newgamesapi.game.GameBehavior;
import org.madblock.newgamesapi.game.GameHandler;
import org.madblock.newgamesapi.team.TeamPresets;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class GameEventListeners implements Listener {

    protected final BlockSwapGameBehaviour behaviour;
    private final List<UUID> cooldown = new ArrayList<>(); // Required bc win 10 is wack. Triggers interact event multiple times.

    public GameEventListeners(BlockSwapGameBehaviour behaviour) {
        this.behaviour = behaviour;
    }

    @EventHandler
    public void onItemMovement(InventoryTransactionEvent event) {
        Player player = event.getTransaction().getSource();
        if (this.behaviour.getSessionHandler().getPlayers().contains(player) && player.getGamemode() != Player.CREATIVE) {
            event.setCancelled();
        }
    }

    @EventHandler
    public void onAbilityUsage(PlayerInteractEvent event) {
        Item item = event.getItem();
        if (this.behaviour.getSessionHandler().getPlayers().contains(event.getPlayer()) && item.hasCompoundTag()) {
            if (!this.behaviour.getSessionHandler().getGameState().equals(GameHandler.GameState.MAIN_LOOP)) {
                return;
            }
            if (this.cooldown.contains(event.getPlayer().getLoginChainData().getClientUUID())) {
                return;
            }
            CompoundTag nbtTag = item.getNamedTag();
            String abilityTag = nbtTag.getString("ability");
            if (abilityTag.equals("leap")) {
                // Apply cooldown
                this.cooldown.add(event.getPlayer().getLoginChainData().getClientUUID());
                this.behaviour.getSessionHandler().getGameScheduler().registerGameTask(() -> {
                    this.cooldown.remove(event.getPlayer().getLoginChainData().getClientUUID());
                }, 10);
                if (!nbtTag.exist("ench")) {
                    event.getPlayer().sendMessage(Utility.generateServerMessage("ABILITY", TextFormat.RED, "This ability is still on cooldown!"));
                    return;
                }

                // Apply leap motion
                Vector3 directionVector = event.getPlayer().getDirectionVector();
                event.getPlayer().setMotion(new Vector3(directionVector.getX(), Math.abs(directionVector.getY()) / 2, directionVector.getZ()).multiply(BlockSwapConstants.LEAP_STRENGTH));

                // Remove ability usage
                nbtTag.remove("ench");
                item.setCompoundTag(nbtTag);
                event.getPlayer().getInventory().setItemInHand(item);
                event.getPlayer().getInventory().sendContents(event.getPlayer());

                // Allow usage of the ability after 2s
                int itemIndex = event.getPlayer().getInventory().getHeldItemIndex();
                this.behaviour.getSessionHandler().getGameScheduler().registerGameTask(() -> {
                    if (!this.behaviour.getSessionHandler().getTeams().get(TeamPresets.DEAD_TEAM_ID).getPlayers().contains(event.getPlayer())) {
                        nbtTag.putList(new ListTag<>("ench"));
                        item.setCompoundTag(nbtTag);
                        Inventory inventory = event.getPlayer().getInventory();
                        inventory.setItem(itemIndex, item);
                        inventory.sendContents(event.getPlayer());
                    }
                }, 40);
            } else if (abilityTag.equals("power_up")) {
                PowerUp powerUp = this.behaviour.getPowerUp(event.getPlayer());
                Inventory inventory = event.getPlayer().getInventory();

                for (int i = 0; i < 9; i++) {
                    Item hotBarItem = inventory.getItem(i);
                    if (hotBarItem.getId() == Block.WOOL) {
                        hotBarItem.clearCustomName();
                        CompoundTag hotBarTag = hotBarItem.getNamedTag();
                        hotBarTag.remove("ench");
                        hotBarTag.remove("ability");
                        hotBarItem.setNamedTag(hotBarTag);
                        inventory.setItem(i, hotBarItem);
                    }
                }
                inventory.sendContents(event.getPlayer());

                if (powerUp != null) {
                    for (Player p : behaviour.getSessionHandler().getPlayers()) {
                        p.sendMessage(Utility.generateServerMessage("POWERUP", TextFormat.YELLOW, String.format("%s%s used %s%s%s%s%s!", event.getPlayer().getDisplayName(), TextFormat.GRAY, TextFormat.BOLD, TextFormat.YELLOW, powerUp.getName(), TextFormat.RESET, TextFormat.GRAY )));
                    }
                    powerUp.use();
                    this.behaviour.setPowerUp(event.getPlayer(), null);
                    this.behaviour.getSessionHandler().getScoreboardManager().setLine(event.getPlayer(), BlockSwapConstants.SCOREBOARD_POWERUP_INDEX, String.format("Power Up:%s None", TextFormat.GRAY));
                }
            }
        }
    }

    @EventHandler
    public void onPowerUpEntityAttacked(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && this.behaviour.getSessionHandler().getPlayers().contains((Player) event.getDamager())) {
            Player player = (Player) event.getDamager();

            if (this.behaviour.isPowerUpEntity(event.getEntity()) && !this.behaviour.getSessionHandler().getTeams().get(TeamPresets.DEAD_TEAM_ID).getPlayers().contains(player)) {
                Class<? extends PowerUp> powerUpClass = BlockSwapUtility.getRandomPowerUp();
                PowerUp powerUp;
                try {
                    powerUp = powerUpClass.getConstructor(GameBehavior.class, Player.class).newInstance(this.behaviour, player);
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException exception) {
                    BlockSwapPlugin.getInstance().getLogger().error(String.format("[PowerUps] Could not create %s.\n%s", powerUpClass.getName(), exception.toString()));
                    return;
                }

                this.behaviour.removePowerUpEntity(event.getEntity());
                event.getEntity().despawnFromAll();
                event.getDamager().getLevel().addSound(new Vector3(event.getDamager().getX(), event.getDamager().getY(), event.getDamager().getZ()), Sound.FIREWORK_SHOOT, 1, 1, player);

                for (Player p : this.behaviour.getSessionHandler().getPlayers()) {
                    p.sendMessage(Utility.generateServerMessage("POWERUP", TextFormat.YELLOW, String.format("%s%s has received the %s%s%s%s%s power up!", ((Player)event.getDamager()).getDisplayName(), TextFormat.GRAY, TextFormat.BOLD, TextFormat.YELLOW, powerUp.getName(), TextFormat.RESET, TextFormat.GRAY )));
                }
                player.sendMessage(Utility.generateServerMessage("POWERUP", TextFormat.YELLOW, String.format("%s%s", TextFormat.AQUA, powerUp.getDescription())));

                if (powerUp.isInstantConsumable()) {
                    powerUp.use();
                } else {
                    this.behaviour.setPowerUp(player, powerUp);
                    Inventory inventory = player.getInventory();
                    this.behaviour.getSessionHandler().getScoreboardManager().setLine(player, BlockSwapConstants.SCOREBOARD_POWERUP_INDEX, String.format("Power Up:%s %s", TextFormat.YELLOW, powerUp.getName()));

                    for (int i = 0; i < 9; i++) {
                        Item item = inventory.getItem(i);
                        if (item.getId() == Block.WOOL) {
                            item.setCustomName(BlockSwapUtility.getPowerUpItemName(powerUp));
                            CompoundTag tag = item.getNamedTag();
                            tag.putList(new ListTag<>("ench"));
                            tag.putString("ability", "power_up");
                            item.setNamedTag(tag);
                            inventory.setItem(i, item);
                        }
                    }

                    inventory.sendContents(player);
                }
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity().getLevel().getId() == this.behaviour.getSessionHandler().getPrimaryMap().getId()) {
            if (event.getCause().equals(DamageCause.LIGHTNING) || event.getCause().equals(DamageCause.FALL)) {
                event.setCancelled();
            } else if (event.getCause().equals(DamageCause.FIRE_TICK)) {
                event.getEntity().extinguish();
                event.setCancelled();
            }
        }
    }

}
