package org.madblock.place.behavior;

import cn.nukkit.Player;
import cn.nukkit.block.BlockID;
import cn.nukkit.block.BlockWool;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.inventory.InventoryTransactionEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.level.Sound;
import cn.nukkit.nbt.tag.IntTag;
import cn.nukkit.utils.DyeColor;
import cn.nukkit.utils.TextFormat;
import org.madblock.newgamesapi.game.GameBehavior;
import org.madblock.newgamesapi.game.events.GamePlayerDeathEvent;
import org.madblock.newgamesapi.team.Team;
import org.madblock.newgamesapi.util.Utility;
import org.madblock.place.PlacePlugin;
import org.madblock.place.util.PlaceUtils;

import java.util.*;

public class PlaceBehavior extends GameBehavior {

    protected static final int COOLDOWN_SECONDS_PER_PIXEL = 30;
    protected static final int COOLDOWN_SECONDS_PER_MESSAGE = 1;

    protected final Set<UUID> superUsers = new HashSet<>();
    protected final Map<UUID, Long> cooldowns = new HashMap<>();
    protected final Map<UUID, Long> lastCooldownMessage = new HashMap<>();

    public PlaceBehavior() {
        PlacePlugin.get().getServer().getScheduler().scheduleRepeatingTask(PlacePlugin.get(), this::updateTimerScoreboard, 10);
    }

    @Override
    public void onInitialCountdownEnd() {

    }

    @Override
    public void registerGameSchedulerTasks() {

    }

    @Override
    public void onSuper(Player player) {
        this.lastCooldownMessage.remove(player.getUniqueId());
        this.cooldowns.remove(player.getUniqueId());

        this.superUsers.add(player.getUniqueId());
    }

    @Override
    public void onPlayerLeaveGame(Player player) {
        this.superUsers.remove(player.getUniqueId());
    }

    @Override
    public void onGameDeathByBlock(GamePlayerDeathEvent event) {
        this.onDeath(event);
    }

    @Override
    public void onGameDeathByEntity(GamePlayerDeathEvent event) {
        this.onDeath(event);
    }

    @Override
    public void onGameDeathByPlayer(GamePlayerDeathEvent event) {
        this.onDeath(event);
    }

    @Override
    public void onGameMiscDeathEvent(GamePlayerDeathEvent event) {
        this.onDeath(event);
    }

    protected void onDeath(GamePlayerDeathEvent event){
        event.setShowDeathMessage(false);
        event.setDeathState(GamePlayerDeathEvent.DeathState.INSTANT_RESPAWN);
    }

    @Override
    public Team.GenericTeamBuilder[] getTeams() {
        return new Team.GenericTeamBuilder[] {
                Team.newBasicTeamBuilder("players", "Players", Team.Colour.WHITE)
                        .setCanDealDamage(false)
                        .setCanPlayersPickUpItems(false)
                        .setCanPlayersDropItems(false)
                        .setFlightEnabled(true)
                        .setFriendlyFireEnabled(false)
        };
    }

    @EventHandler
    public void onSwapPage(PlayerInteractEvent event) {
        boolean isValidSwapInteraction = this.getSessionHandler().getPlayers().contains(event.getPlayer())
                && event.getPlayer().getInventory().getItemInHand().getId() == ItemID.HEART_OF_THE_SEA;

        if (isValidSwapInteraction) {
            IntTag pageTag = (IntTag) event.getPlayer().getInventory().getItemInHand().getNamedTagEntry("page");
            if (pageTag != null) {
                int page = pageTag.getData();

                Item[] hotbarItems;
                if (page == 0) {
                    hotbarItems = PlaceUtils.getSecondPage();
                } else {
                    hotbarItems = PlaceUtils.getFirstPage();
                }

                // Update hotbar items to next page
                event.getPlayer().getInventory().clearAll();
                for (int i = 0; i < hotbarItems.length; i++) {
                    event.getPlayer().getInventory().setItem(i, hotbarItems[i], false);
                }
                event.getPlayer().getInventory().sendContents(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onWoolInteract(PlayerInteractEvent event) {
        boolean isValidInteraction = event.getBlock().getId() == BlockID.WOOL
                && this.getSessionHandler().getPlayers().contains(event.getPlayer())
                && event.getPlayer().getInventory().getItemInHand().getId() != ItemID.HEART_OF_THE_SEA;

        if (isValidInteraction) {
            if (this.isOnCooldown(event.getPlayer())) {
                boolean shouldShowCooldownMessage = !this.lastCooldownMessage.containsKey(event.getPlayer().getUniqueId())
                        || this.lastCooldownMessage.get(event.getPlayer().getUniqueId()) < System.currentTimeMillis();

                if (shouldShowCooldownMessage) {
                    this.lastCooldownMessage.put(event.getPlayer().getUniqueId(), System.currentTimeMillis() + (COOLDOWN_SECONDS_PER_MESSAGE * 1000));
                    this.sendCooldownMessage(event.getPlayer());
                }
                return;
            } else {
                this.lastCooldownMessage.remove(event.getPlayer().getUniqueId());
            }

            if (!this.superUsers.contains(event.getPlayer().getUniqueId())) {
                this.cooldowns.put(event.getPlayer().getUniqueId(), System.currentTimeMillis() + (COOLDOWN_SECONDS_PER_PIXEL * 1000));
                this.getSessionHandler()
                        .getGameScheduler()
                        .registerGameTask(() -> this.cooldowns.remove(event.getPlayer().getUniqueId()), COOLDOWN_SECONDS_PER_PIXEL * 20);
            }

            Item itemInHand = event.getPlayer().getInventory().getItemInHand();
            DyeColor chosenColor = DyeColor.getByDyeData(itemInHand.getDamage());

            this.getSessionHandler().getPrimaryMap().setBlock(event.getBlock().getLocation(), new BlockWool(chosenColor.getWoolData()));
            event.getPlayer().getLevel().addSound(event.getPlayer().getLocation(), Sound.LAND_SLIME);
        }
    }

    protected void updateTimerScoreboard() {
        for (Player player : this.getSessionHandler().getPlayers()) {
            int seconds = this.getCooldownSecondsLeft(player);

            if (seconds > 0) {
                this.getSessionHandler().getScoreboardManager().setLine(player, 0, Utility.ResourcePackCharacters.TIME + " " + seconds + "s");
                player.sendActionBar(TextFormat.RED + "" + seconds + "s until you can place another pixel...");
            } else {
                this.getSessionHandler().getScoreboardManager().setLine(player, 0, TextFormat.GREEN + "Click a wool block!");
                player.sendActionBar(TextFormat.GREEN + "Tap a wool block with dye!");
            }
        }
    }

    protected int getCooldownSecondsLeft(Player player) {
        if (this.cooldowns.containsKey(player.getUniqueId()) && !this.superUsers.contains(player.getUniqueId())) {
            boolean onCooldown = this.cooldowns.get(player.getUniqueId()) > System.currentTimeMillis();

            if (onCooldown) {
                return (int) Math.ceil((this.cooldowns.get(player.getUniqueId()) - System.currentTimeMillis()) / 1000d);
            }
        }

        return 0;
    }

    protected boolean isOnCooldown(Player player) {
        return this.getCooldownSecondsLeft(player) > 0;
    }

    protected void sendCooldownMessage(Player player) {
        int secondsLeft = (int) Math.ceil((this.cooldowns.get(player.getUniqueId()) - System.currentTimeMillis()) / 1000d);
        player.sendMessage(Utility.generateServerMessage("COOLDOWN", TextFormat.DARK_RED, String.format("You are on cooldown for %s seconds!", secondsLeft), TextFormat.RED));
        player.getLevel().addSound(player.getLocation(), Sound.NOTE_BASS, 1, 1, player);
    }

    @EventHandler
    public void onInventoryMove(InventoryTransactionEvent event) {
        if (this.getSessionHandler().getPlayers().contains(event.getTransaction().getSource())) {
            event.setCancelled();
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (this.getSessionHandler().getPlayers().contains(event.getPlayer())) {
            event.setCancelled();
        }
    }

}
